(ns feuerwehr-strichliste.util)

(defn format-date [iso]
  (.toLocaleString (js/Date. iso) "de-DE"
                   #js {:day "2-digit" :month "2-digit" :year "2-digit"
                        :hour "2-digit" :minute "2-digit"}))

(defn local-date-str
  "Returns a YYYY-MM-DD string in local time for the given Date object."
  [^js d]
  (str (.getFullYear d) "-"
       (-> (.getMonth d) inc (.toString) (.padStart 2 "0")) "-"
       (-> (.getDate d) (.toString) (.padStart 2 "0"))))

(defn default-date-from []
  (let [d (js/Date.)]
    (.setFullYear d (- (.getFullYear d) 1))
    (local-date-str d)))

(defn default-date-to []
  (local-date-str (js/Date.)))
