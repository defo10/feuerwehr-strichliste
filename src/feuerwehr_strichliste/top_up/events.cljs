(ns feuerwehr-strichliste.top-up.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::admin-top-up
 (fn-traced [{:keys [db]} [_ {:keys [user-id amount]}]]
            (let [actor-id (get-in db [:ui :current-user-id])
                  role     (get-in db [:snapshot :users actor-id :user/role])
                  ts       (.toISOString (js/Date.))]
              (if (permissions/can? role :confirm-top-ups)
                (let [{snapshot1 :snapshot req-event :event}
                      (reducer/apply-event
                       (:snapshot db)
                       (fn [id]
                         {:event/type      :balance/top-up-requested
                          :event/id        id
                          :event/timestamp ts
                          :event/actor     actor-id
                          :event/subject   user-id
                          :top-up/amount   amount}))
                      {snapshot2 :snapshot confirm-event :event}
                      (reducer/apply-event
                       snapshot1
                       (fn [id]
                         {:event/type        :balance/top-up-confirmed
                          :event/id          id
                          :event/timestamp   ts
                          :event/actor       actor-id
                          :event/subject     user-id
                          :top-up/request-id (:event/id req-event)}))
                      events [req-event confirm-event]]
                  {:db       (assoc db :snapshot snapshot2)
                   :persist! {:events events :snapshot snapshot2}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-db
 ::stage-top-up
 (fn-traced [db [_ {:keys [user-id amount]}]]
            (assoc-in db [:ui :pending-top-up] {:user-id user-id :amount amount})))

(re-frame/reg-event-db
 ::clear-staged-top-up
 (fn-traced [db _]
            (assoc-in db [:ui :pending-top-up] nil)))

(re-frame/reg-event-db
 ::open-top-up-form
 (fn-traced [db _]
            (assoc-in db [:ui :top-up-editing?] true)))

(re-frame/reg-event-db
 ::close-top-up-form
 (fn-traced [db _]
            (assoc-in db [:ui :top-up-editing?] false)))

(re-frame/reg-event-fx
 ::confirm-top-up
 (fn-traced [{:keys [db]} [_ {:keys [request-id user-id]}]]
            (let [actor-id   (get-in db [:ui :current-user-id])
                  actor-role (get-in db [:snapshot :users actor-id :user/role])]
              (if (permissions/can? actor-role :confirm-top-ups)
                (let [{:keys [snapshot event]}
                      (reducer/apply-event
                       (:snapshot db)
                       (fn [id]
                         {:event/type        :balance/top-up-confirmed
                          :event/id          id
                          :event/timestamp   (.toISOString (js/Date.))
                          :event/actor       actor-id
                          :event/subject     user-id
                          :top-up/request-id request-id}))]
                  {:db       (assoc db :snapshot snapshot)
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-fx
 ::cancel-top-up
 (fn-traced [{:keys [db]} [_ {:keys [request-id user-id]}]]
            (let [actor-id    (get-in db [:ui :current-user-id])
                  actor-role  (get-in db [:snapshot :users actor-id :user/role])
                  history     (get-in db [:snapshot :users user-id :user/history])
                  entry       (first (filter #(= request-id (:history/id %)) history))
                  can-cancel? (case (:history/status entry)
                                :active    (or (= actor-id (:history/actor entry))
                                               (permissions/can? actor-role :confirm-top-ups))
                                :confirmed (and (= actor-id (:history/actor entry))
                                                (permissions/can? actor-role :confirm-top-ups))
                                false)]
              (if can-cancel?
                (let [{:keys [snapshot event]}
                      (reducer/apply-event
                       (:snapshot db)
                       (fn [id]
                         {:event/type        :balance/top-up-cancelled
                          :event/id          id
                          :event/timestamp   (.toISOString (js/Date.))
                          :event/actor       actor-id
                          :event/subject     user-id
                          :top-up/request-id request-id
                          :top-up/amount     (:top-up/amount entry)}))]
                  {:db       (assoc db :snapshot snapshot)
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
