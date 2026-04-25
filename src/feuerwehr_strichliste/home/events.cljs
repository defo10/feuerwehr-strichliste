(ns feuerwehr-strichliste.home.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   ["bcryptjs" :as bcrypt]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::set-search-query
 (fn-traced [db [_ query]]
   (assoc-in db [:ui :search-query] query)))

(re-frame/reg-event-db
 ::open-pin-modal
 (fn-traced [db [_ user]]
   (assoc-in db [:ui :pin] {:user user :digits "" :error nil :success false})))

(re-frame/reg-event-db
 ::close-pin-modal
 (fn-traced [db _]
   (assoc-in db [:ui :pin :user] nil)))

(re-frame/reg-event-db
 ::pin-backspace
 (fn-traced [db _]
   (update-in db [:ui :pin :digits]
              #(subs % 0 (max 0 (dec (count %)))))))

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
