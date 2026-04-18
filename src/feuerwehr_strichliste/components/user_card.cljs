(ns feuerwehr-strichliste.components.user-card)

(defn user-card [{:user/keys [name]}]
  [:div.user-list-item name])
