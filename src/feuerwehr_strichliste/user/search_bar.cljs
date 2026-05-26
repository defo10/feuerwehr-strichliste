(ns feuerwehr-strichliste.user.search-bar)

(defn search-bar [{:keys [value on-change]}]
  [:input.search-bar
   {:type        "search"
    :placeholder "Filtern..."
    :value       value
    :on-change   #(on-change (.. % -target -value))}])
