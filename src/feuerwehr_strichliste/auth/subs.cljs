(ns feuerwehr-strichliste.auth.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::pin-state
 (fn [db _]
   (get-in db [:ui :pin])))
