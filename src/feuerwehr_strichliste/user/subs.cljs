(ns feuerwehr-strichliste.user.subs
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as app-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

(re-frame/reg-sub
 ::users-map
 (fn [db _]
   (get-in db [:snapshot :users])))

(re-frame/reg-sub
 ::editing-user
 (fn [db _]
   (get-in db [:ui :editing-user])))

(re-frame/reg-sub
 ::profile-open?
 (fn [db _]
   (get-in db [:ui :profile-open?])))

(re-frame/reg-sub
 ::can-manage-users?
 :<- [::app-subs/current-user]
 (fn [user _]
   (permissions/can? (:user/role user) :manage-users)))

(re-frame/reg-sub
 ::all-users
 :<- [::users-map]
 (fn [users-map _]
   (->> users-map vals (sort-by :user/name))))
