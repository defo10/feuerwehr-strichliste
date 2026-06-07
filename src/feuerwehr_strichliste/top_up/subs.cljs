(ns feuerwehr-strichliste.top-up.subs
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.user.subs :as user-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

(re-frame/reg-sub
 ::top-ups-map
 :<- [::user-subs/users-map]
 (fn [users-map _]
   (into {}
         (for [[uid user] users-map
               entry      (:user/history user)
               :when      (= :top-up (:history/type entry))]
           [(:history/id entry)
            {:top-up/id           (:history/id entry)
             :top-up/user-id      uid
             :top-up/amount       (:top-up/amount entry)
             :top-up/requested-at (:history/timestamp entry)
             :top-up/requested-by (:history/actor entry)
             :top-up/status       (case (:history/status entry)
                                    :active    :pending
                                    :confirmed :confirmed
                                    :cancelled :cancelled)
             :top-up/reviewed-at (:top-up/reviewed-at entry)
             :top-up/reviewed-by (:top-up/reviewed-by entry)}]))))

(re-frame/reg-sub
 ::top-ups-sorted
 :<- [::top-ups-map]
 (fn [top-ups-map _]
   (sort-by (juxt #(if (= :pending (:top-up/status %)) 0 1)
                  #(- (.getTime (js/Date. (:top-up/requested-at %)))))
            (vals top-ups-map))))

(re-frame/reg-sub
 ::can-confirm-top-ups?
 :<- [::app-subs/current-user]
 (fn [user _]
   (permissions/can? (:user/role user) :confirm-top-ups)))

(re-frame/reg-sub
 ::pending-top-up
 (fn [db _] (get-in db [:ui :pending-top-up])))

(re-frame/reg-sub
 ::top-up-editing?
 (fn [db _] (get-in db [:ui :top-up-editing?])))
