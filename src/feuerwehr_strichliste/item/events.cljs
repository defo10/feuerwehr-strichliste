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
            (let [item-ids (->> (vals (get-in db [:domain :items]))
                                (keep :item/image-key))]
              (when (seq item-ids)
                {:load-item-images! item-ids}))))

(re-frame/reg-event-db
 ::set-active-tab
 (fn-traced [db [_ tab]]
            (assoc-in db [:ui :active-tab] tab)))

(re-frame/reg-event-db
 ::increment
 (fn-traced [db [_ item-id]]
            (let [stock (get-in db [:domain :items item-id :item/stock])
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
            (let [cart  (get-in db [:ui :cart])
                  items (get-in db [:domain :items])
                  pairs (->> cart
                             (keep (fn [[item-id qty]]
                                     (when (pos? qty)
                                       (when-let [item (get items item-id)]
                                         {:item item :quantity qty}))))
                             (sort-by #(get-in % [:item :item/name])))
                  total (reduce (fn [sum {:keys [item quantity]}]
                                  (+ sum (* quantity (:item/price item))))
                                0 pairs)]
              (assoc-in db [:ui :receipt] {:entries pairs :total total}))))

(re-frame/reg-event-db
 ::dismiss-receipt
 (fn-traced [db _]
            (assoc-in db [:ui :receipt] nil)))

(re-frame/reg-event-fx
 ::confirm-checkout
 (fn-traced [{:keys [db]} _]
            (let [entries  (get-in db [:ui :receipt :entries])
                  user-id  (get-in db [:ui :current-user-id])
                  {:keys [domain event]} (reducer/apply-event
                                          (:domain db)
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
              {:db       (assoc db :domain domain)
               :persist! {:event event :snapshot domain}})))

(re-frame/reg-event-fx
 ::item-create
 (fn-traced [{:keys [db]} [_ {:item/keys [type name price stock] :keys [image]}]]
            (let [user-id (get-in db [:ui :current-user-id])
                  role    (get-in db [:domain :users user-id :user/role])]
              (if (permissions/can? role :manage-items)
                (let [{:keys [domain event]} (reducer/apply-event
                                              (:domain db)
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
                      db'     (cond-> (assoc db :domain domain)
                                image (assoc-in [:ui :item-images item-id]
                                               (js/URL.createObjectURL image)))]
                  (merge {:db       db'
                          :persist! {:event event :snapshot domain}}
                         (when image {:persist-image! {:item-id item-id :blob image}})))
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
