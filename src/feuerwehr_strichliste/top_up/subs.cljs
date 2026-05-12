(ns feuerwehr-strichliste.top-up.subs
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as app-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

(re-frame/reg-sub
 ::top-ups-map
 (fn [db _]
   (get-in db [:snapshot :top-ups])))

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
