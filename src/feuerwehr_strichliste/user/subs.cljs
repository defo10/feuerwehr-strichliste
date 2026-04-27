(ns feuerwehr-strichliste.user.subs
  (:require [re-frame.core :as re-frame]
            [clojure.string :as str]
            [feuerwehr-strichliste.subs :as app-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

(re-frame/reg-sub
 ::users-map
 (fn [db _]
   (get-in db [:domain :users])))

(re-frame/reg-sub
 ::search-query
 (fn [db _]
   (get-in db [:ui :search-query])))

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
 ::users
 :<- [::users-map]
 :<- [::search-query]
 (fn [[users-map query] _]
   (let [q        (str/lower-case (or query ""))
         matches? #(str/includes? (str/lower-case (:user/name %)) q)]
     (->> users-map vals (filter matches?) (sort-by :user/name)))))

(re-frame/reg-sub
 ::all-users
 :<- [::users-map]
 (fn [users-map _]
   (->> users-map vals (sort-by :user/name))))

(re-frame/reg-sub
 ::users-by-letter
 :<- [::users]
 (fn [users _]
   (sort-by first (group-by #(first (:user/name %)) users))))

(re-frame/reg-sub
 ::used-letters
 :<- [::users-by-letter]
 (fn [by-letter _]
   (set (map first by-letter))))
