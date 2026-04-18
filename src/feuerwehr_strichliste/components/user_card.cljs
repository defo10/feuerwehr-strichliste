(ns feuerwehr-strichliste.components.user-card)

(defn user-card [{:user/keys [name active?]}]
  [:div {:class (str "user-list-item" (when-not active? " inactive"))}
   name])
