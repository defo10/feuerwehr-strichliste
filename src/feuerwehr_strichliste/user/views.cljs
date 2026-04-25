(ns feuerwehr-strichliste.user.views
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.auth.events :as auth-events]))

(defn user-card [user]
  [:div.user-list-item
   {:on-click #(re-frame/dispatch [::auth-events/open-pin-modal user])}
   (:user/name user)])
