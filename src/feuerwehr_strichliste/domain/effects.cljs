(ns feuerwehr-strichliste.domain.effects
  (:require [re-frame.core :as re-frame]
            [konserve.core :as k]
            [cljs.core.async :refer [go <!]]
            [feuerwehr-strichliste.domain.storage :as storage]
            [feuerwehr-strichliste.config :as config]))

(re-frame/reg-fx
 :persist!
 (fn [{:keys [events snapshot]}]
   (go
     (try
       (doseq [event events]
         (<! (k/append @storage/store :event-log event)))
       (<! (k/assoc-in @storage/store [:snapshot] snapshot))
       (catch :default e
         (re-frame/dispatch [:error :errors/persist-failed (.-message e)])
         (when config/debug?
           (js/setTimeout #(throw e) 0)))))))

(re-frame/reg-fx
 :load-activity-log!
 (fn [_]
   (go
     (try
       (let [log (<! (k/reduce-log @storage/store :event-log conj []))]
         (re-frame/dispatch [:activity-log/loaded log]))
       (catch :default e
         (re-frame/dispatch [:error :errors/load-failed (.-message e)]))))))

(re-frame/reg-fx
 :dispatch-after
 (fn [{:keys [ms dispatch]}]
   (js/setTimeout #(re-frame/dispatch dispatch) ms)))

(re-frame/reg-fx
 :persist-image!
 (fn [{:keys [item-id blob]}]
   (go
     (try
       (<! (k/assoc-in @storage/store [:item-images item-id] blob))
       (catch :default e
         (re-frame/dispatch [:error :errors/persist-failed (.-message e)]))))))
