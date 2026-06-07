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
 ::activity-log
 (fn [db _]
   (get-in db [:ui :activity-log])))

(re-frame/reg-sub
 ::user-events-for
 (fn [db [_ user-id]]
   (rseq (get-in db [:snapshot :users user-id :user/history] []))))

(re-frame/reg-sub
 ::user-history
 :<- [::current-user]
 (fn [user _]
   (when user
     (->> (:user/history user)
          (group-by :history/timestamp)
          (sort-by key)
          reverse
          (map val)))))
