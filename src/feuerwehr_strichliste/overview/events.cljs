(ns feuerwehr-strichliste.overview.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

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

(re-frame/reg-event-fx
 :command/create-user
 (fn-traced [{:keys [db]} [_ {:keys [name role pin-hash]}]]
            (let [{:keys [domain event]} (reducer/apply-event
                                          (:domain db)
                                          (fn [id]
                                            {:event/type      :user/created
                                             :event/id        id
                                             :event/timestamp (.toISOString (js/Date.))
                                             :event/actor     (get-in db [:ui :current-user-id])
                                             :user/name       name
                                             :user/role       role
                                             :user/pin-hash   pin-hash}))]
              {:db       (assoc db :domain domain)
               :persist! {:event event :snapshot domain}})))
