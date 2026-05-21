(ns feuerwehr-strichliste.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::error
 (fn [db _]
   (get-in db [:ui :error])))

(re-frame/reg-sub
 ::current-user
 (fn [db _]
   (get-in db [:snapshot :users (get-in db [:ui :current-user-id])])))

(re-frame/reg-sub
 ::current-user-balance
 (fn [db _]
   (get-in db [:snapshot :balances (get-in db [:ui :current-user-id])] 0)))

(re-frame/reg-sub
 ::all-balances
 (fn [db _]
   (get-in db [:snapshot :balances])))

(re-frame/reg-sub
 ::event-log
 (fn [db _]
   (get db :event-log)))

(re-frame/reg-sub
 ::user-history
 :<- [::event-log]
 :<- [::current-user]
 (fn [[event-log user] _]
   (when user
     (let [uid (:user/id user)]
       (->> event-log
            (filter #(or (and (= :cart/checked-out (:event/type %))
                              (= uid (:event/actor %)))
                         (and (= :balance/top-up-requested (:event/type %))
                              (= uid (or (:event/subject %) (:top-up/user-id %))))))
            (group-by :event/timestamp)
            (sort-by key)
            reverse
            (map val))))))
