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
 ::current-user-id
 (fn [db _]
   (get-in db [:ui :current-user-id])))

(re-frame/reg-sub
 ::current-user
 (fn [db _]
   (get-in db [:domain :users (get-in db [:ui :current-user-id])])))
