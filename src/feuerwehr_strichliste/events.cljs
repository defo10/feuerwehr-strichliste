(ns feuerwehr-strichliste.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.db :as db]
   [feuerwehr-strichliste.storage :as storage]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [konserve.core :as k]
   [cljs.core.async :refer [go <!]]
   [feuerwehr-strichliste.config :as config]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
 ::navigate
 (fn-traced [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-db
 :error
 (fn-traced [db [_ type message]]
   (assoc-in db [:ui :error] {:type type :message message})))

(re-frame/reg-event-db
 :error/dismiss
 (fn-traced [db _]
   (assoc-in db [:ui :error] nil)))

(re-frame/reg-event-fx
 ::sign-out
 (fn-traced [{:keys [db]} _]
   (let [{:keys [domain event]} (reducer/apply-event
                                  (:domain db)
                                  (fn [id]
                                    {:event/type      :auth/signed-out
                                     :event/id        id
                                     :event/timestamp (.toISOString (js/Date.))
                                     :event/actor     (get-in db [:ui :current-user-id])}))]
     {:db       (-> db
                    (assoc :domain domain)
                    (assoc-in [:ui :current-user-id] nil)
                    (assoc-in [:ui :pin] {:user nil :digits "" :error nil :success false}))
      :persist! {:event event :snapshot domain}
      :navigate :home})))

(re-frame/reg-fx
 :persist-image!
 (fn [{:keys [item-id blob]}]
   (go
     (try
       (<! (k/assoc-in @storage/store [:item-images item-id] blob))
       (catch :default e
         (re-frame/dispatch [:error :errors/persist-failed (.-message e)]))))))

(re-frame/reg-fx
 :persist!
 (fn [{:keys [event snapshot]}]
   (go
     (try
       (<! (k/append @storage/store :event-log event))
       (<! (k/assoc-in @storage/store [:snapshot] snapshot))
       (catch :default e
         (re-frame/dispatch [:error :errors/persist-failed (.-message e)])
         ;; go blocks swallow exceptions — setTimeout re-throws outside core.async's
         ;; try/catch so the error reaches the global handler and is visible in dev tools
         (when config/debug?
           (js/setTimeout #(throw e) 0)))))))
