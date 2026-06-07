(ns feuerwehr-strichliste.pages.home
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.auth.subs :as auth-subs]
   [feuerwehr-strichliste.user.views :refer [user-card]]
   [feuerwehr-strichliste.user.alphabet-bar :refer [alphabet-bar]]
   [feuerwehr-strichliste.user.search-bar :refer [search-bar]]
   [feuerwehr-strichliste.auth.views :refer [pin-modal]]))

(defn home-page []
  (let [all-users    (re-frame/subscribe [::user-subs/all-users])
        pin-state    (re-frame/subscribe [::auth-subs/pin-state])
        rfid-toast   (re-frame/subscribe [::auth-subs/rfid-toast])
        search-query (r/atom "")]
    (fn []
      (let [{:keys [user] :as pin} @pin-state
            toast        @rfid-toast
            q            (str/lower-case (or @search-query ""))
            users        (if (str/blank? q)
                           @all-users
                           (filter #(str/includes? (str/lower-case (:user/name %)) q) @all-users))
            by-letter    (sort-by first (group-by #(first (:user/name %)) users))
            used-letters (set (map first by-letter))]
        [:div.page
         [:div.page-header
          [:h1.page-title "Wer bist du?"]
          [search-bar {:value     @search-query
                       :on-change #(reset! search-query %)}]]
         [:div.user-list
          (for [[letter group] by-letter]
            [:div {:id (str "letter-" letter) :key letter}
             (for [u group]
               ^{:key (:user/id u)} [user-card u])])]
         [alphabet-bar {:used-letters used-letters :id-prefix "letter-"}]
         (when user [pin-modal pin])
         (when toast
           [:div.rfid-toast {:class (str "rfid-toast--" (name (:type toast)))}
            (:message toast)])]))))
