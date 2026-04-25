(ns feuerwehr-strichliste.user.search-bar
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.user.events :as events]
            [feuerwehr-strichliste.user.subs :as subs]))

(defn search-bar []
  (let [query (re-frame/subscribe [::subs/search-query])]
    [:input.search-bar
     {:type        "search"
      :placeholder "Filtern..."
      :value       @query
      :on-change   #(re-frame/dispatch [::events/set-search-query (.. % -target -value)])}]))
