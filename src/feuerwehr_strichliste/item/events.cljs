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

                      [snapshot2 correction-event]
                      (if stock-changed?
                        (let [{:keys [snapshot event]}
                              (reducer/apply-event
                               snapshot1
                               (fn [ev-id]
                                 {:event/type         :item/stock-corrected
                                  :event/id           ev-id
                                  :event/timestamp    ts
                                  :event/actor        user-id
                                  :item/id            id
                                  :item/stock-before  (:item/stock current)
                                  :item/stock         stock}))]
                          [snapshot event])
                        [snapshot1 nil])

                      events (filterv some? [edit-event correction-event])]
                  (if (seq events)
                    (let [base (assoc db :snapshot snapshot2)
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

(re-frame/reg-event-fx
 ::confirm-checkout
 (fn-traced [{:keys [db]} _]
            (let [cart       (get-in db [:ui :cart])
                  items      (get-in db [:snapshot :items])
                  user-id    (get-in db [:ui :current-user-id])
                  pending-tu (get-in db [:ui :pending-top-up])
                  ts         (.toISOString (js/Date.))
                  entries    (->> cart
                                  (keep (fn [[item-id qty]]
                                          (when (pos? qty)
                                            (when-let [item (get items item-id)]
                                              {:item-id    item-id
                                               :quantity   qty
                                               :unit-price (:item/price item)}))))
                                  vec)
                  [snap1 cart-event]
                  (if (seq entries)
                    (let [{:keys [snapshot event]}
                          (reducer/apply-event
                           (:snapshot db)
                           (fn [id]
                             {:event/type       :cart/checked-out
                              :event/id         id
                              :event/timestamp  ts
                              :event/actor      user-id
                              :checkout/entries entries}))]
                      [snapshot event])
                    [(:snapshot db) nil])
                  [snap2 tu-event]
                  (if pending-tu
                    (let [{:keys [snapshot event]}
                          (reducer/apply-event
                           snap1
                           (fn [id]
                             {:event/type      :balance/top-up-requested
                              :event/id        id
                              :event/timestamp ts
                              :event/actor     user-id
                              :event/subject   (:user-id pending-tu)
                              :top-up/amount   (:amount pending-tu)}))]
                      [snapshot event])
                    [snap1 nil])
                  events (filterv some? [cart-event tu-event])]
              (if (seq events)
                {:db       (-> db
                               (assoc :snapshot snap2)
                               (assoc-in [:ui :cart] {})
                               (assoc-in [:ui :pending-top-up] nil))
                 :persist! {:events events :snapshot snap2}}
                {:db (-> db
                         (assoc-in [:ui :cart] {})
                         (assoc-in [:ui :pending-top-up] nil))}))))

(re-frame/reg-event-fx
 ::void-checkout
 (fn-traced [{:keys [db]} [_ original-event-id]]
            (let [actor-id (get-in db [:ui :current-user-id])
                  role     (get-in db [:snapshot :users actor-id :user/role])
                  original (first (for [[uid user] (get-in db [:snapshot :users])
                                        entry      (:user/history user)
                                        :when      (and (= :checkout (:history/type entry))
                                                        (= original-event-id (:history/id entry)))]
                                    {:uid             uid
                                     :checkout/entries (:checkout/entries entry)}))]
              (if-not (permissions/can? role :void-transaction)
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}
                (if-not original
                  {:db (assoc-in db [:ui :error] {:type :errors/not-found :message "Event not found"})}
                  (let [{:keys [snapshot event]}
                        (reducer/apply-event
                         (:snapshot db)
                         (fn [id]
                           {:event/type       :transaction/voided
                            :event/id         id
                            :event/timestamp  (.toISOString (js/Date.))
                            :event/actor      actor-id
                            :event/subject    (:uid original)
                            :void/original-id original-event-id
                            :checkout/entries (:checkout/entries original)}))]
                    {:db       (assoc db :snapshot snapshot)
                     :persist! {:events [event] :snapshot snapshot}}))))))

(re-frame/reg-event-fx
 ::admin-checkout
 (fn-traced [{:keys [db]} [_ {:keys [user-id entries]}]]
            (let [actor-id (get-in db [:ui :current-user-id])
                  role     (get-in db [:snapshot :users actor-id :user/role])]
              (if-not (permissions/can? role :manage-users)
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}
                (if (empty? entries)
                  {:db db}
                  (let [{:keys [snapshot event]}
                        (reducer/apply-event
                         (:snapshot db)
                         (fn [id]
                           {:event/type       :cart/checked-out
                            :event/id         id
                            :event/timestamp  (.toISOString (js/Date.))
                            :event/actor      actor-id
                            :event/subject    user-id
                            :checkout/entries entries}))]
                    {:db       (assoc db :snapshot snapshot)
                     :persist! {:events [event] :snapshot snapshot}}))))))

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
                      db'     (cond-> (assoc db :snapshot snapshot)
                                image (assoc-in [:ui :item-images item-id]
                                                (js/URL.createObjectURL image)))]
                  (merge {:db       db'
                          :persist! {:events [event] :snapshot snapshot}}
                         (when image {:persist-image! {:item-id item-id :blob image}})))
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
