(ns feuerwehr-strichliste.item.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

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

(re-frame/reg-event-fx
 ::checkout
 (fn-traced [{:keys [db]} _]
   (let [cart    (get-in db [:ui :cart])
         items   (get-in db [:domain :items])
         user-id (get-in db [:ui :current-user-id])
         pairs   (->> cart
                      (keep (fn [[item-id qty]]
                              (when (pos? qty)
                                (when-let [item (get items item-id)]
                                  {:item item :quantity qty}))))
                      (sort-by #(get-in % [:item :item/name])))
         entries (map (fn [{:keys [item quantity]}]
                        {:item-id    (:item/id item)
                         :quantity   quantity
                         :unit-price (:item/price item)})
                      pairs)
         total   (reduce (fn [sum {:keys [item quantity]}]
                           (+ sum (* quantity (:item/price item))))
                         0 pairs)
         {:keys [domain event]} (reducer/apply-event
                                 (:domain db)
                                 (fn [id]
                                   {:event/type       :cart/checked-out
                                    :event/id         id
                                    :event/timestamp  (.toISOString (js/Date.))
                                    :event/actor      user-id
                                    :checkout/entries (vec entries)}))]
     {:db       (-> db
                    (assoc :domain domain)
                    (assoc-in [:ui :cart] {})
                    (assoc-in [:ui :receipt] {:entries pairs :total total}))
      :persist! {:event event :snapshot domain}})))

(re-frame/reg-event-fx
 ::item-create
 (fn-traced [{:keys [db]} [_ {:item/keys [type name price stock] :keys [image]}]]
   (let [{:keys [domain event]} (reducer/apply-event
                                 (:domain db)
                                 (fn [id]
                                   (merge {:event/type      :item/created
                                           :event/id        id
                                           :event/timestamp (.toISOString (js/Date.))
                                           :event/actor     (get-in db [:ui :current-user-id])
                                           :item/type       type
                                           :item/name       name
                                           :item/price      price
                                           :item/stock      stock}
                                          (when image {:item/image-key id}))))]
     (merge {:db       (assoc db :domain domain)
             :persist! {:event event :snapshot domain}}
            (when image {:persist-image! {:item-id (:event/id event) :blob image}})))))
