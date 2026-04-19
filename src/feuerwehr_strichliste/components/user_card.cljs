(ns feuerwehr-strichliste.components.user-card
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.events :as events]))

(defn user-card [user]
  [:div.user-list-item
   {:on-click #(re-frame/dispatch [::events/open-pin-modal user])}
   (:user/name user)])
