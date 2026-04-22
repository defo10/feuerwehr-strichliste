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

(re-frame/reg-event-fx
 ::item-create
 (fn-traced [{:keys [db]} [_ {:item/keys [type name price stock] :keys [image]}]]
            (let [{:keys [domain event]} (reducer/apply-event
                                          (:domain db)
                                          (fn [id]
                                            (merge {:event/type      :item/created
                                                    :event/id        id
                                                    :event/timestamp (.toISOString (js/Date.))
                                                    :event/actor     (get-in db [:ui :current-user-id])
                                                    :item/type       type
                                                    :item/name       name
                                                    :item/price      price
                                                    :item/stock      stock}
                                                   (when image {:item/image-key id}))))]
              (merge {:db       (assoc db :domain domain)
                      :persist! {:event event :snapshot domain}}
                     (when image {:persist-image! {:item-id (:event/id event) :blob image}})))))

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
                                              (fn [id]
                                                {:event/type      :auth/sign-in-attempted
                                                 :event/id        id
                                                 :event/timestamp (.toISOString (js/Date.))
                                                 :event/actor     (:user/id user)
                                                 :auth/success    valid?}))]
                  (if valid?
                    {:db       (-> db'
                                   (assoc :domain domain)
                                   (assoc-in [:ui :current-user-id] (:user/id user)))
                     :persist! {:event event :snapshot domain}
                     :navigate :overview}
                    {:db       (-> db'
                                   (assoc :domain domain)
                                   (assoc-in [:ui :pin :digits] "")
                                   (assoc-in [:ui :pin :error] "Falsche PIN"))
                     :persist! {:event event :snapshot domain}}))))))

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

(re-frame/reg-event-db
 ::pin-backspace
 (fn-traced [db _]
            (update-in db [:ui :pin :digits]
                       #(subs % 0 (max 0 (dec (count %)))))))

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
