(ns feuerwehr-strichliste.domain.commands
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.domain.reducer :as reducer]))

(re-frame/reg-event-fx
 :item/create
 (fn [{:keys [db]} [_ {:keys [type name price stock]}]]
   (let [{:keys [domain event]} (reducer/apply-event
                                 (:domain db)
                                 {:event/type      :item/created
                                  :event/timestamp (.toISOString (js/Date.))
                                  :event/actor     (get-in db [:ui :current-user-id])
                                  :item/type       type
                                  :item/name       name
                                  :item/price      price
                                  :item/stock      stock})]
     {:db       (assoc db :domain domain)
      :persist! {:event event :snapshot domain}})))

(re-frame/reg-event-fx
 :command/create-user
 (fn [{:keys [db]} [_ {:keys [name role pin-hash]}]]
   (let [{:keys [domain event]} (reducer/apply-event
                                 (:domain db)
                                 {:event/type      :user/created
                                  :event/timestamp (.toISOString (js/Date.))
                                  :user/name       name
                                  :user/role       role
                                  :user/pin-hash   pin-hash})]
     {:db       (assoc db :domain domain)
      :persist! {:event event :snapshot domain}})))
