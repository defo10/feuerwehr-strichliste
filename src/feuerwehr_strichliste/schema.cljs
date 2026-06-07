(ns feuerwehr-strichliste.schema
  (:require
   ["bcryptjs" :as bcrypt]
   [clojure.test.check.generators :as gen]
   [malli.generator :as mg]))

(comment
  (.hashSync bcrypt "1234" 10))

(def ^:private german-names
  ["Alexander Bauer" "Maria Hoffmann" "Thomas Fischer" "Lena Wagner"
   "Klaus Müller" "Anna Schmidt" "Stefan Schneider" "Julia Weber"
   "Markus Meyer" "Sarah Schulz" "Michael Koch" "Laura Richter"
   "Andreas Wolf" "Katharina Braun" "Christian Zimmermann" "Sabine Krause"
   "Thomas Hartmann" "Nina Lehmann" "Florian Neumann" "Monika Schwarz"
   "Hans Bergmann" "Petra Lange" "Jürgen Kremer" "Birgit Schäfer"
   "Wolfgang Engel" "Christine Vogt" "Rainer Beckmann" "Ursula Pohl"
   "Dieter Franke" "Ingrid Albrecht" "Günter Sommer" "Renate Haase"
   "Herbert Brandt" "Hildegard Seifert" "Manfred Böhm" "Gertrude Winter"
   "Horst Schumacher" "Elfriede Gruber" "Egon Pfeiffer" "Waltraud Stein"])

(def CheckoutEntry
  [:map
   [:item-id    string?]
   [:quantity   pos-int?]
   [:unit-price pos-int?]])

(def CheckoutHistoryEntry
  [:map
   [:history/id        string?]
   [:history/type      [:= :checkout]]
   [:history/timestamp string?]
   [:history/actor     string?]
   [:checkout/entries  [:sequential CheckoutEntry]]])

(def TopUpHistoryEntry
  [:map
   [:history/id        string?]
   [:history/type      [:= :top-up]]
   [:history/timestamp string?]
   [:history/actor     string?]
   [:top-up/amount     pos-int?]])

(def HistoryEntry
  [:or CheckoutHistoryEntry TopUpHistoryEntry])

(def UserHistory
  [:and
   [:vector HistoryEntry]
   [:fn {:error/message "history entries must be in chronological order"}
    (fn [entries]
      (let [ts (map :history/timestamp entries)]
        (every? true? (map #(<= (compare %1 %2) 0) ts (rest ts)))))]])

(def User
  [:map
   [:user/id       string?]
   [:user/name     [:string {:gen/gen (gen/elements german-names)}]]
   [:user/role     [:enum :member :kitchen :admin]]
   [:user/pin-hash [:string {:gen/gen (gen/return "$2b$10$fSviXQEHvZ/dHtwvKUREbOFZcc9Recla6YM4vFMmgbLb9hyNLpij.")}]]
   [:user/status   [:enum :active :inactive :suspended]]
   [:user/history  {:gen/gen (gen/return [])} UserHistory]])

(defn generate-users [n]
  (into {}
        (map-indexed
         (fn [_ user]
           (let [id (str (random-uuid))]
             [id (assoc user :user/id id :user/role :admin)]))
         (mg/sample User {:size n}))))

(def Item
  [:map
   [:item/id        string?]
   [:item/type      [:enum :drink :food]]
   [:item/name      string?]
   [:item/price     pos-int?]
   [:item/stock     nat-int?]
   [:item/status    [:enum :active :inactive]]
   [:item/image-key {:optional true} string?]])

(def UserCreatedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :user/created]]
   [:user/name       string?]
   [:user/role       [:enum :member :kitchen :admin]]
   [:user/pin-hash   string?]])

(def ItemCreatedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :item/created]]
   [:item/type       [:enum :drink :food]]
   [:item/name       string?]
   [:item/price      pos-int?]
   [:item/stock      nat-int?]
   [:item/image-key  {:optional true} string?]])

(def AuthSignInAttemptedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :auth/sign-in-attempted]]
   [:auth/success    boolean?]])

(def AuthSignedOutEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :auth/signed-out]]])

(def CartCheckedOutEvent
  [:map
   [:event/id         string?]
   [:event/timestamp  string?]
   [:event/actor      string?]
   [:event/type       [:= :cart/checked-out]]
   [:event/subject    {:optional true} string?]
   [:checkout/entries [:sequential CheckoutEntry]]])

(def ItemEditedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :item/edited]]
   [:item/id         string?]
   [:item/type       [:enum :drink :food]]
   [:item/name       string?]
   [:item/price      pos-int?]
   [:item/image-key  {:optional true} string?]])

(def ItemRestockedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :item/restocked]]
   [:item/id         string?]
   [:item/stock      nat-int?]])

(def UserUpdatedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :user/updated]]
   [:user/id         string?]
   [:user/name       string?]
   [:user/role       [:enum :member :kitchen :admin]]
   [:user/status     [:enum :active :inactive :suspended]]
   [:user/pin-hash   {:optional true} string?]])

(def TopUpRequestedEvent
  [:map
   [:event/id        string?]
   [:event/timestamp string?]
   [:event/actor     string?]
   [:event/type      [:= :balance/top-up-requested]]
   [:event/subject   string?]
   [:top-up/amount   pos-int?]])

(def TopUpConfirmedEvent
  [:map
   [:event/id           string?]
   [:event/timestamp    string?]
   [:event/actor        string?]
   [:event/type         [:= :balance/top-up-confirmed]]
   [:top-up/request-id  string?]])

(def TopUpCancelledEvent
  [:map
   [:event/id           string?]
   [:event/timestamp    string?]
   [:event/actor        string?]
   [:event/type         [:= :balance/top-up-cancelled]]
   [:event/subject      string?]
   [:top-up/request-id  string?]
   [:top-up/amount      pos-int?]])

(def TopUp
  [:map
   [:top-up/id           string?]
   [:top-up/user-id      string?]
   [:top-up/amount       pos-int?]
   [:top-up/requested-at string?]
   [:top-up/requested-by string?]
   [:top-up/status       [:enum :pending :confirmed :cancelled]]
   [:top-up/confirmed-at {:optional true} string?]
   [:top-up/confirmed-by {:optional true} string?]
   [:top-up/cancelled-at {:optional true} string?]
   [:top-up/cancelled-by {:optional true} string?]])

(def TransactionVoidedEvent
  [:map
   [:event/id          string?]
   [:event/timestamp   string?]
   [:event/actor       string?]
   [:event/type        [:= :transaction/voided]]
   [:event/subject     string?]
   [:void/original-id  string?]
   [:void/reason       {:optional true} string?]
   [:checkout/entries  [:sequential CheckoutEntry]]])

(def DomainEvent
  [:multi {:dispatch :event/type}
   [:user/created              UserCreatedEvent]
   [:user/updated              UserUpdatedEvent]
   [:item/created              ItemCreatedEvent]
   [:item/edited               ItemEditedEvent]
   [:item/restocked            ItemRestockedEvent]
   [:cart/checked-out          CartCheckedOutEvent]
   [:auth/sign-in-attempted    AuthSignInAttemptedEvent]
   [:auth/signed-out           AuthSignedOutEvent]
   [:balance/top-up-requested  TopUpRequestedEvent]
   [:balance/top-up-confirmed  TopUpConfirmedEvent]
   [:balance/top-up-cancelled  TopUpCancelledEvent]
   [:transaction/voided        TransactionVoidedEvent]])

(comment
  (generate-users 1))
