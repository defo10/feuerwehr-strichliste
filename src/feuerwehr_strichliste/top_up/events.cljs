(ns feuerwehr-strichliste.top-up.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::request-top-up
 (fn-traced [{:keys [db]} [_ {:keys [user-id amount]}]]
   (let [actor-id (get-in db [:ui :current-user-id])
         {:keys [domain event]}
         (reducer/apply-event
          (:domain db)
          (fn [id]
            {:event/type       :balance/top-up-requested
             :event/id         id
             :event/timestamp  (.toISOString (js/Date.))
             :event/actor      actor-id
             :top-up/user-id   user-id
             :top-up/amount    amount}))]
     {:db       (assoc db :domain domain)
      :persist! {:event event :snapshot domain}})))

(re-frame/reg-event-fx
 ::confirm-top-up
 (fn-traced [{:keys [db]} [_ request-id]]
   (let [actor-id   (get-in db [:ui :current-user-id])
         actor-role (get-in db [:domain :users actor-id :user/role])]
     (if (permissions/can? actor-role :confirm-top-ups)
       (let [{:keys [domain event]}
             (reducer/apply-event
              (:domain db)
              (fn [id]
                {:event/type        :balance/top-up-confirmed
                 :event/id          id
                 :event/timestamp   (.toISOString (js/Date.))
                 :event/actor       actor-id
                 :top-up/request-id request-id}))]
         {:db       (assoc db :domain domain)
          :persist! {:event event :snapshot domain}})
       {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-fx
 ::cancel-top-up
 (fn-traced [{:keys [db]} [_ request-id]]
   (let [actor-id     (get-in db [:ui :current-user-id])
         actor-role   (get-in db [:domain :users actor-id :user/role])
         top-up       (get-in db [:domain :top-ups request-id])
         can-cancel?  (and (= :pending (:top-up/status top-up))
                           (or (= actor-id (:top-up/requested-by top-up))
                               (permissions/can? actor-role :confirm-top-ups)))]
     (if can-cancel?
       (let [{:keys [domain event]}
             (reducer/apply-event
              (:domain db)
              (fn [id]
                {:event/type        :balance/top-up-cancelled
                 :event/id          id
                 :event/timestamp   (.toISOString (js/Date.))
                 :event/actor       actor-id
                 :top-up/request-id request-id}))]
         {:db       (assoc db :domain domain)
          :persist! {:event event :snapshot domain}})
       {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
