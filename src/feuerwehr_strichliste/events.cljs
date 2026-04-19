(ns feuerwehr-strichliste.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.db :as db]
   [feuerwehr-strichliste.storage :as storage]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   [konserve.core :as k]
   [cljs.core.async :refer [go <!]]
   [feuerwehr-strichliste.config :as config]
   ["bcryptjs" :as bcrypt]
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
 ::set-search-query
 (fn-traced [db [_ query]]
   (assoc-in db [:ui :search-query] query)))

(re-frame/reg-event-db
 :error
 (fn-traced [db [_ type message]]
   (assoc-in db [:ui :error] {:type type :message message})))

(re-frame/reg-event-db
 :error/dismiss
 (fn-traced [db _]
   (assoc-in db [:ui :error] nil)))

(re-frame/reg-event-db
 ::open-pin-modal
 (fn-traced [db [_ user]]
   (assoc-in db [:ui :pin] {:user user :digits "" :error nil :success false})))

(re-frame/reg-event-db
 ::close-pin-modal
 (fn-traced [db _]
   (assoc-in db [:ui :pin :user] nil)))

(re-frame/reg-event-fx
 ::pin-digit
 (fn-traced [{:keys [db]} [_ digit]]
   (let [current   (get-in db [:ui :pin :digits])
         new-pin   (if (< (count current) 4) (str current digit) current)
         complete? (= 4 (count new-pin))
         db'       (-> db
                       (assoc-in [:ui :pin :digits] new-pin)
                       (assoc-in [:ui :pin :error] nil))]
     (if-not complete?
       {:db db'}
       (let [user                   (get-in db [:ui :pin :user])
             valid?                 (bcrypt/compareSync new-pin (:user/pin-hash user))
             {:keys [domain event]} (reducer/apply-event
                                     (:domain db)
                                     {:event/type      :auth/sign-in-attempted
                                      :event/timestamp (.toISOString (js/Date.))
                                      :event/actor     (:user/id user)
                                      :auth/success    valid?})]
         {:db       (-> db'
                        (assoc :domain domain)
                        (cond-> valid?       (-> (assoc-in [:ui :pin :success] true)
                                                 (assoc-in [:ui :current-user-id] (:user/id user))))
                        (cond-> (not valid?) (-> (assoc-in [:ui :pin :digits] "")
                                                 (assoc-in [:ui :pin :error] "Falsche PIN"))))
          :persist! {:event event :snapshot domain}})))))

(re-frame/reg-event-db
 ::pin-backspace
 (fn-traced [db _]
   (update-in db [:ui :pin :digits]
              #(subs % 0 (max 0 (dec (count %)))))))

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
