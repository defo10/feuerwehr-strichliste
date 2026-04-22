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

(def User
  [:map
   [:user/id       pos-int?]
   [:user/name     [:string {:gen/gen (gen/elements german-names)}]]
   [:user/role     [:enum :member :kitchen :admin]]
   [:user/pin-hash [:string {:gen/gen (gen/return "$2b$10$fSviXQEHvZ/dHtwvKUREbOFZcc9Recla6YM4vFMmgbLb9hyNLpij.")}]]
   [:user/status   [:enum :active :inactive :suspended]]])

(defn generate-users [n]
  (into {}
        (map-indexed
         (fn [i user]
           (let [id (inc i)]
             [id (assoc user :user/id id :user/role :admin)]))
         (mg/sample User {:size n}))))

(def Item
  [:map
   [:item/id        nat-int?]
   [:item/type      [:enum :drink :food]]
   [:item/name      string?]
   [:item/price     pos-int?]
   [:item/stock     nat-int?]
   [:item/status    [:enum :active :inactive]]
   [:item/image-key {:optional true} nat-int?]])

(def UserCreatedEvent
  [:map
   [:event/id        nat-int?]
   [:event/timestamp string?]
   [:event/actor     nat-int?]
   [:event/type      [:= :user/created]]
   [:user/name       string?]
   [:user/role       [:enum :member :kitchen :admin]]
   [:user/pin-hash   string?]])

(def ItemCreatedEvent
  [:map
   [:event/id        nat-int?]
   [:event/timestamp string?]
   [:event/actor     nat-int?]
   [:event/type      [:= :item/created]]
   [:item/type       [:enum :drink :food]]
   [:item/name       string?]
   [:item/price      pos-int?]
   [:item/stock      nat-int?]
   [:item/image-key  {:optional true} nat-int?]])

(def AuthSignInAttemptedEvent
  [:map
   [:event/id        nat-int?]
   [:event/timestamp string?]
   [:event/actor     nat-int?]
   [:event/type      [:= :auth/sign-in-attempted]]
   [:auth/success    boolean?]])

(def AuthSignedOutEvent
  [:map
   [:event/id        nat-int?]
   [:event/timestamp string?]
   [:event/actor     nat-int?]
   [:event/type      [:= :auth/signed-out]]])

(def DomainEvent
  [:multi {:dispatch :event/type}
   [:user/created           UserCreatedEvent]
   [:item/created           ItemCreatedEvent]
   [:auth/sign-in-attempted AuthSignInAttemptedEvent]
   [:auth/signed-out        AuthSignedOutEvent]])

(def Snapshot
  [:map
   [:users          [:map-of nat-int? User]]
   [:balances       [:map-of nat-int? number?]]
   [:items          [:map-of nat-int? Item]]
   [:next-event-id  nat-int?]])

(comment
  (generate-users 1))