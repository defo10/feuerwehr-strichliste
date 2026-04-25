(ns feuerwehr-strichliste.user.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::set-search-query
 (fn-traced [db [_ query]]
   (assoc-in db [:ui :search-query] query)))

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
