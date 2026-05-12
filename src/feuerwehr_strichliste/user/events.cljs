(ns feuerwehr-strichliste.user.events
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as str]
   ["bcryptjs" :as bcrypt]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::set-search-query
 (fn-traced [db [_ query]]
   (assoc-in db [:ui :search-query] query)))

(re-frame/reg-event-db
 ::edit-user
 (fn-traced [db [_ user]]
            (assoc-in db [:ui :editing-user] user)))

(re-frame/reg-event-db
 ::close-edit
 (fn-traced [db _]
            (assoc-in db [:ui :editing-user] nil)))

(re-frame/reg-event-db
 ::open-profile
 (fn-traced [db _]
            (assoc-in db [:ui :profile-open?] true)))

(re-frame/reg-event-db
 ::close-profile
 (fn-traced [db _]
            (assoc-in db [:ui :profile-open?] false)))

(re-frame/reg-event-fx
 ::user-create
 (fn-traced [{:keys [db]} [_ {:keys [name role pin]}]]
            (let [actor-id   (get-in db [:ui :current-user-id])
                  actor-role (get-in db [:snapshot :users actor-id :user/role])]
              (if (permissions/can? actor-role :manage-users)
                (let [{:keys [snapshot event]}
                      (reducer/apply-event
                       (:snapshot db)
                       (fn [id]
                         {:event/type      :user/created
                          :event/id        id
                          :event/timestamp (.toISOString (js/Date.))
                          :event/actor     actor-id
                          :user/name       name
                          :user/role       role
                          :user/pin-hash   (.hashSync bcrypt pin 10)}))]
                  {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

(re-frame/reg-event-fx
 ::user-update
 (fn-traced [{:keys [db]} [_ {:keys [id name role status pin]}]]
            (let [actor-id   (get-in db [:ui :current-user-id])
                  actor-role (get-in db [:snapshot :users actor-id :user/role])
                  self?      (= actor-id id)]
              (if (or self? (permissions/can? actor-role :manage-users))
                (let [current (get-in db [:snapshot :users id])
                      {:keys [snapshot event]}
                      (reducer/apply-event
                       (:snapshot db)
                       (fn [ev-id]
                         (cond-> {:event/type      :user/updated
                                  :event/id        ev-id
                                  :event/timestamp (.toISOString (js/Date.))
                                  :event/actor     actor-id
                                  :user/id         id
                                  :user/name       name
                                  :user/role       (or role (:user/role current))
                                  :user/status     (or status (:user/status current))}
                           (not (str/blank? pin))
                           (assoc :user/pin-hash (.hashSync bcrypt pin 10)))))]
                  {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
                   :persist! {:events [event] :snapshot snapshot}})
                {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))

;; Legacy event kept for compatibility
(re-frame/reg-event-fx
 :command/create-user
 (fn-traced [{:keys [db]} [_ {:keys [name role pin-hash]}]]
   (let [{:keys [snapshot event]} (reducer/apply-event
                                   (:snapshot db)
                                   (fn [id]
                                     {:event/type      :user/created
                                      :event/id        id
                                      :event/timestamp (.toISOString (js/Date.))
                                      :event/actor     (get-in db [:ui :current-user-id])
                                      :user/name       name
                                      :user/role       role
                                      :user/pin-hash   pin-hash}))]
     {:db       (assoc db :snapshot snapshot :event-log (conj (:event-log db) event))
      :persist! {:events [event] :snapshot snapshot}})))
