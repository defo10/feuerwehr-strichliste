(ns feuerwehr-strichliste.schema
  (:require [malli.core :as m]
            [malli.generator :as mg]
            [clojure.test.check.generators :as gen]))

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
   [:user/pin-hash [:string {:min 1}]]
   [:user/status   [:enum :active :inactive :suspended]]])

(defn generate-users [n]
  (into {}
        (map-indexed
         (fn [i user]
           (let [id (inc i)]
             [id (assoc user :user/id id)]))
         (mg/sample User {:size n}))))
