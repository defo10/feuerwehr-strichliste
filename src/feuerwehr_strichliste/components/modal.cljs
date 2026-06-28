(ns feuerwehr-strichliste.components.modal)

(defn modal [{:keys [on-close]} & children]
  [:div.confirm-overlay {:on-click on-close}
   (into [:div.confirm-modal {:on-click #(.stopPropagation %)}]
         children)])
