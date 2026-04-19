(ns feuerwehr-strichliste.pages.overview
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.events :as events]))

(defn overview-page []
  (let [current-user (re-frame/subscribe [::subs/current-user])]
    (fn []
      [:div
       [:nav.top-nav
        [:span.top-nav-name (:user/name @current-user)]
        [:button.top-nav-logout
         {:on-click #(re-frame/dispatch [::events/sign-out])}
         "Fertig"]]])))
