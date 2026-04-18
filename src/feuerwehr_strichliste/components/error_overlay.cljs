(ns feuerwehr-strichliste.components.error-overlay
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as subs]))

(defn error-overlay []
  (let [error (re-frame/subscribe [::subs/error])]
    (when @error
      [:div.error-overlay
       [:div.error-dialog
        [:h2.error-title "Fehler"]
        [:p.error-message (:message @error)]
        [:div.error-actions
         [:button.error-reload
          {:on-click #(.reload js/location)}
          "Neu laden"]
         [:button.error-dismiss
          {:on-click #(re-frame/dispatch [:error/dismiss])}
          "Schließen"]]]])))
