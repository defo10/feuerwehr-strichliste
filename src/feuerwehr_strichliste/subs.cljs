(ns feuerwehr-strichliste.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::users-map
 (fn [db _]
   (get-in db [:domain :users])))

(re-frame/reg-sub
 ::users
 :<- [::users-map]
 (fn [users-map _]
   (->> users-map vals (sort-by :user/name))))

(re-frame/reg-sub
 ::current-user-id
 (fn [db _]
   (get-in db [:ui :current-user-id])))

(re-frame/reg-sub
 ::current-user
 :<- [::users-map]
 :<- [::current-user-id]
 (fn [[users-map id] _]
   (get users-map id)))
