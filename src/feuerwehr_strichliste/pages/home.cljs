(ns feuerwehr-strichliste.pages.home
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.components.user-card :refer [user-card]]
   [feuerwehr-strichliste.components.alphabet-bar :refer [alphabet-bar]]
   [feuerwehr-strichliste.components.search-bar :refer [search-bar]]
   [feuerwehr-strichliste.components.pin-modal :refer [pin-modal]]))

(defn home-page []
  (let [by-letter (re-frame/subscribe [::subs/users-by-letter])
        pin-state (re-frame/subscribe [::subs/pin-state])]
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
