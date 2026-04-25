(ns feuerwehr-strichliste.pages.home
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.auth.subs :as auth-subs]
   [feuerwehr-strichliste.user.views :refer [user-card]]
   [feuerwehr-strichliste.user.alphabet-bar :refer [alphabet-bar]]
   [feuerwehr-strichliste.user.search-bar :refer [search-bar]]
   [feuerwehr-strichliste.auth.views :refer [pin-modal]]))

(defn home-page []
  (let [by-letter (re-frame/subscribe [::user-subs/users-by-letter])
        pin-state (re-frame/subscribe [::auth-subs/pin-state])]
    (fn []
      (let [{:keys [user] :as pin} @pin-state]
        [:div.page
         [:div.page-header
          [:h1.page-title "Wer bist du?"]
          [search-bar]]
         [:div.user-list
          (for [[letter users] @by-letter]
            [:div {:id (str "letter-" letter) :key letter}
             (for [u users]
               ^{:key (:user/id u)} [user-card u])])]
         [alphabet-bar]
         (when user [pin-modal pin])]))))
