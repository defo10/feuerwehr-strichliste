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
 ::search-query
 (fn [db _]
   (get-in db [:ui :search-query])))

(re-frame/reg-sub
 ::users
 :<- [::users-map]
 :<- [::search-query]
 (fn [[users-map query] _]
   (let [q (clojure.string/lower-case (or query ""))
         matches? #(clojure.string/includes?
                    (clojure.string/lower-case (:user/name %)) q)]
     (->> users-map vals (filter matches?) (sort-by :user/name)))))

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

(re-frame/reg-sub
 ::error
 (fn [db _]
   (get-in db [:ui :error])))

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
