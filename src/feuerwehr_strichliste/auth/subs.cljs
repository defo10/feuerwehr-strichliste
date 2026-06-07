(ns feuerwehr-strichliste.auth.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::pin-state
 (fn [db _]
   (get-in db [:ui :pin])))

(re-frame/reg-sub
 ::rfid-toast
 (fn [db _]
   (get-in db [:ui :rfid-toast])))

(re-frame/reg-sub
 ::pending-rfid
 (fn [db _]
   (get-in db [:ui :pending-rfid])))
