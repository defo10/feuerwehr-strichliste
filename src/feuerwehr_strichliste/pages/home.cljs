(ns feuerwehr-strichliste.pages.home
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.components.user-card :refer [user-card]]))

(defn home-page []
  (let [users (re-frame/subscribe [::subs/users])]
    [:div.page
     [:h1.page-title "Wer bist du?"]
     [:div.user-list
      (for [user @users]
        ^{:key (:user/id user)} [user-card user])]]))
