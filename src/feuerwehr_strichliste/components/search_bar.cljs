(ns feuerwehr-strichliste.components.search-bar
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.events :as events]
            [feuerwehr-strichliste.subs :as subs]))

(defn search-bar []
  (let [query (re-frame/subscribe [::subs/search-query])]
    [:input.search-bar
     {:type        "search"
      :placeholder "Suchen..."
      :value       @query
      :on-change   #(re-frame/dispatch [::events/set-search-query (.. % -target -value)])}]))
