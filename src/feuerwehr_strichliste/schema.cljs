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
             [id (assoc user :user/id id)]))
         (mg/sample User {:size n}))))
