(ns feuerwehr-strichliste.top-up.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn- find-top-up [event-log request-id]
  (let [request    (first (filter #(and (= :balance/top-up-requested (:event/type %))
                                        (= request-id (:event/id %)))
                                  event-log))
        resolution (first (filter #(and (#{:balance/top-up-confirmed :balance/top-up-cancelled}
                                         (:event/type %))
                                        (= request-id (:top-up/request-id %)))
                                  event-log))]
    (when request
      {:top-up/id           request-id
       :top-up/user-id      (or (:event/subject request) (:top-up/user-id request))
       :top-up/amount       (:top-up/amount request)
       :top-up/requested-by (:event/actor request)
       :top-up/status       (cond
                              (nil? resolution)                                        :pending
                              (= :balance/top-up-confirmed (:event/type resolution))  :confirmed
                              (= :balance/top-up-cancelled (:event/type resolution))  :cancelled)})))

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
                          :top-up/request-id (:event/id req-event)}))
                      events [req-event confirm-event]]
                  {:db       (-> db
                                 (assoc :snapshot snapshot2)
                                 (update :event-log into events))
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
 (fn-traced [{:keys [db]} [_ request-id]]
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
                          :top-up/request-id request-id}))]
                  {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-fx
 ::cancel-top-up
 (fn-traced [{:keys [db]} [_ request-id]]
            (let [actor-id   (get-in db [:ui :current-user-id])
                  actor-role (get-in db [:snapshot :users actor-id :user/role])
                  top-up     (find-top-up (:event-log db) request-id)
                  can-cancel? (case (:top-up/status top-up)
                                :pending   (or (= actor-id (:top-up/requested-by top-up))
                                               (permissions/can? actor-role :confirm-top-ups))
                                :confirmed (and (= actor-id (:top-up/requested-by top-up))
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
                          :event/subject     (:top-up/user-id top-up)
                          :top-up/request-id request-id
                          :top-up/amount     (:top-up/amount top-up)}))]
                  {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
