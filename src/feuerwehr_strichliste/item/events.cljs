(ns feuerwehr-strichliste.item.events
  (:require
   [re-frame.core :as re-frame]
   [konserve.core :as k]
   [cljs.core.async :as async :refer [go <!]]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [feuerwehr-strichliste.domain.storage :as storage]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn- item-image-entry [id]
  (go (when-let [blob (<! (k/get-in @storage/store [:item-images id]))]
        [id (js/URL.createObjectURL blob)])))

(re-frame/reg-fx
 :load-item-images!
 (fn [item-ids]
   (go (->> item-ids
            (map item-image-entry)
            async/merge
            (async/into [])
            <!
            (keep identity)
            (into {})
            (conj [::images-loaded])
            re-frame/dispatch))))

(re-frame/reg-event-db
 ::images-loaded
 (fn-traced [db [_ url-map]]
            (assoc-in db [:ui :item-images] url-map)))

(re-frame/reg-event-fx
 ::load-images
 (fn-traced [{:keys [db]} _]
            (let [item-ids (->> (vals (get-in db [:snapshot :items]))
                                (keep :item/image-key))]
              (when (seq item-ids)
                {:load-item-images! item-ids}))))

(re-frame/reg-event-db
 ::edit-item
 (fn-traced [db [_ item]]
            (assoc-in db [:ui :editing-item] item)))

(re-frame/reg-event-db
 ::close-edit
 (fn-traced [db _]
            (assoc-in db [:ui :editing-item] nil)))

(re-frame/reg-event-fx
 ::item-update
 (fn-traced [{:keys [db]} [_ {:item/keys [id type name price stock] :keys [image]}]]
            (let [user-id (get-in db [:ui :current-user-id])
                  role    (get-in db [:snapshot :users user-id :user/role])]
              (if (permissions/can? role :manage-items)
                (let [current           (get-in db [:snapshot :items id])
                      metadata-changed? (or image
                                            (not= type  (:item/type current))
                                            (not= name  (:item/name current))
                                            (not= price (:item/price current)))
                      stock-changed?    (not= stock (:item/stock current))
                      ts                (.toISOString (js/Date.))

                      [snapshot1 edit-event]
                      (if metadata-changed?
                        (let [{:keys [snapshot event]}
                              (reducer/apply-event
                               (:snapshot db)
                               (fn [ev-id]
                                 (merge {:event/type      :item/edited
                                         :event/id        ev-id
                                         :event/timestamp ts
                                         :event/actor     user-id
                                         :item/id         id
                                         :item/type       type
                                         :item/name       name
                                         :item/price      price}
                                        (when image {:item/image-key id}))))]
                          [snapshot event])
                        [(:snapshot db) nil])

                      [snapshot2 restock-event]
                      (if stock-changed?
                        (let [{:keys [snapshot event]}
                              (reducer/apply-event
                               snapshot1
                               (fn [ev-id]
                                 {:event/type      :item/restocked
                                  :event/id        ev-id
                                  :event/timestamp ts
                                  :event/actor     user-id
                                  :item/id         id
                                  :item/stock      stock}))]
                          [snapshot event])
                        [snapshot1 nil])

                      events (filterv some? [edit-event restock-event])]
                  (if (seq events)
                    (let [base (-> db (assoc :snapshot snapshot2) (update :event-log into events))
                          db'  (if image
                                 ; revoke the old URL before replacing it — object URLs hold the blob
                                 ; in memory until explicitly revoked or the page unloads
                                 (do (some-> (get-in base [:ui :item-images id]) js/URL.revokeObjectURL)
                                     (assoc-in base [:ui :item-images id] (js/URL.createObjectURL image)))
                                 base)]
                      (merge {:db       db'
                              :persist! {:events events :snapshot snapshot2}}
                             (when image {:persist-image! {:item-id id :blob image}})))
                    {:db db}))
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-db
 ::set-active-tab
 (fn-traced [db [_ tab]]
            (assoc-in db [:ui :active-tab] tab)))

(re-frame/reg-event-db
 ::increment
 (fn-traced [db [_ item-id]]
            (let [stock (get-in db [:snapshot :items item-id :item/stock])
                  qty   (get-in db [:ui :cart item-id] 0)]
              (if (< qty stock)
                (assoc-in db [:ui :cart item-id] (inc qty))
                db))))

(re-frame/reg-event-db
 ::decrement
 (fn-traced [db [_ item-id]]
            (let [qty (get-in db [:ui :cart item-id] 0)]
              (cond
                (zero? qty) db
                (= 1 qty)   (update-in db [:ui :cart] dissoc item-id)
                :else        (update-in db [:ui :cart item-id] dec)))))

(re-frame/reg-event-db
 ::show-receipt
 (fn-traced [db _]
            (let [cart    (get-in db [:ui :cart])
                  items   (get-in db [:snapshot :items])
                  user-id (get-in db [:ui :current-user-id])
                  pairs   (->> cart
                               (keep (fn [[item-id qty]]
                                       (when (pos? qty)
                                         (when-let [item (get items item-id)]
                                           {:item item :quantity qty}))))
                               (sort-by #(get-in % [:item :item/name])))
                  total   (reduce (fn [sum {:keys [item quantity]}]
                                    (+ sum (* quantity (:item/price item))))
                                  0 pairs)
                  top-ups (let [event-log (:event-log db)
                               requests  (into {} (keep (fn [event]
                                                          (when (and (= :balance/top-up-requested (:event/type event))
                                                                     (= user-id (:top-up/user-id event)))
                                                            [(:event/id event) event]))
                                                        event-log))
                               resolved  (into #{} (keep (fn [event]
                                                           (when (#{:balance/top-up-confirmed :balance/top-up-cancelled}
                                                                  (:event/type event))
                                                             (:top-up/request-id event)))
                                                         event-log))]
                           (->> requests
                                (keep (fn [[id event]]
                                        (when-not (resolved id)
                                          {:top-up/id           id
                                           :top-up/amount       (:top-up/amount event)
                                           :top-up/requested-at (:event/timestamp event)})))
                                (sort-by :top-up/requested-at)))]
              (assoc-in db [:ui :receipt] {:entries pairs :total total :top-ups top-ups}))))

(re-frame/reg-event-db
 ::dismiss-receipt
 (fn-traced [db _]
            (assoc-in db [:ui :receipt] nil)))

(re-frame/reg-event-fx
 ::confirm-checkout
 (fn-traced [{:keys [db]} _]
            (let [entries  (get-in db [:ui :receipt :entries])
                  user-id  (get-in db [:ui :current-user-id])
                  {:keys [snapshot event]} (reducer/apply-event
                                            (:snapshot db)
                                            (fn [id]
                                              {:event/type       :cart/checked-out
                                               :event/id         id
                                               :event/timestamp  (.toISOString (js/Date.))
                                               :event/actor      user-id
                                               :checkout/entries (vec (map (fn [{:keys [item quantity]}]
                                                                             {:item-id    (:item/id item)
                                                                              :quantity   quantity
                                                                              :unit-price (:item/price item)})
                                                                           entries))}))]
              {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
               :persist! {:events [event] :snapshot snapshot}})))

(re-frame/reg-event-fx
 ::item-create
 (fn-traced [{:keys [db]} [_ {:item/keys [type name price stock] :keys [image]}]]
            (let [user-id (get-in db [:ui :current-user-id])
                  role    (get-in db [:snapshot :users user-id :user/role])]
              (if (permissions/can? role :manage-items)
                (let [{:keys [snapshot event]} (reducer/apply-event
                                                (:snapshot db)
                                                (fn [id]
                                                  (merge {:event/type      :item/created
                                                          :event/id        id
                                                          :event/timestamp (.toISOString (js/Date.))
                                                          :event/actor     user-id
                                                          :item/type       type
                                                          :item/name       name
                                                          :item/price      price
                                                          :item/stock      stock}
                                                         (when image {:item/image-key id}))))
                      item-id (:event/id event)
                      db'     (cond-> (-> db (assoc :snapshot snapshot) (update :event-log conj event))
                                image (assoc-in [:ui :item-images item-id]
                                                (js/URL.createObjectURL image)))]
                  (merge {:db       db'
                          :persist! {:events [event] :snapshot snapshot}}
                         (when image {:persist-image! {:item-id item-id :blob image}})))
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
