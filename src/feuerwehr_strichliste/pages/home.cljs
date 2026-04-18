(ns feuerwehr-strichliste.pages.home
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.components.user-card :refer [user-card]]
   [feuerwehr-strichliste.components.alphabet-bar :refer [alphabet-bar]]
   [feuerwehr-strichliste.components.search-bar :refer [search-bar]]))

(defn home-page []
  (let [by-letter (re-frame/subscribe [::subs/users-by-letter])]
    [:div.page
     [:div.page-header
      [:h1.page-title "Wer bist du?"]
      [search-bar]]
     [:div.user-list
      (for [[letter users] @by-letter]
        [:div {:id (str "letter-" letter) :key letter}
         (for [user users]
           ^{:key (:user/id user)} [user-card user])])]
     [alphabet-bar]]))
