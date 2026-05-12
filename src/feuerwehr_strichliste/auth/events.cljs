(ns feuerwehr-strichliste.auth.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.domain.reducer :as reducer]
   ["bcryptjs" :as bcrypt]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

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
       (let [user                       (get-in db [:ui :pin :user])
             valid?                     (bcrypt/compareSync new-pin (:user/pin-hash user))
             {:keys [snapshot event]}   (reducer/apply-event
                                         (:snapshot db)
                                         (fn [id]
                                           {:event/type      :auth/sign-in-attempted
                                            :event/id        id
                                            :event/timestamp (.toISOString (js/Date.))
                                            :event/actor     (:user/id user)
                                            :auth/success    valid?}))]
         (if valid?
           {:db       (-> db'
                          (assoc :snapshot snapshot)
                          (assoc-in [:ui :current-user-id] (:user/id user)))
            :persist! {:events [event] :snapshot snapshot}
            :navigate :overview}
           {:db       (-> db'
                          (assoc :snapshot snapshot)
                          (assoc-in [:ui :pin :digits] "")
                          (assoc-in [:ui :pin :error] "Falsche PIN"))
            :persist! {:events [event] :snapshot snapshot}}))))))

(re-frame/reg-event-fx
 ::sign-out
 (fn-traced [{:keys [db]} _]
   (let [{:keys [snapshot event]} (reducer/apply-event
                                    (:snapshot db)
                                    (fn [id]
                                      {:event/type      :auth/signed-out
                                       :event/id        id
                                       :event/timestamp (.toISOString (js/Date.))
                                       :event/actor     (get-in db [:ui :current-user-id])}))]
     {:db       (-> db
                    (assoc :snapshot snapshot)
                    (assoc-in [:ui :current-user-id] nil)
                    (assoc-in [:ui :pin] {:user nil :digits "" :error nil :success false})
                    (assoc-in [:ui :cart] {})
                    (assoc-in [:ui :receipt] nil)
                    (assoc-in [:ui :active-tab] :drink))
      :persist! {:events [event] :snapshot snapshot}
      :navigate :home})))
