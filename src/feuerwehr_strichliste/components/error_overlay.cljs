(ns feuerwehr-strichliste.components.error-overlay
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as subs]))

(defn error-overlay []
  (let [error (re-frame/subscribe [::subs/error])]
    (when @error
      [:div.modal.is-active
       [:div.modal-background]
       [:div.modal-card
        [:header.modal-card-head
         [:p.modal-card-title "Fehler"]]
        [:section.modal-card-body
         [:p (:message @error)]]
        [:footer.modal-card-foot
         [:div.buttons
          [:button.button.is-danger
           {:on-click #(.reload js/location)}
           "Neu laden"]
          [:button.button
           {:on-click #(re-frame/dispatch [:error/dismiss])}
           "Schließen"]]]]])))
