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
   [feuerwehr-strichliste.auth.views :refer [pin-modal]]
   [feuerwehr-strichliste.user.events :as user-events]))

(defn- onboarding-modal []
  (let [form (r/atom {:name "" :pin ""})]
    (fn []
      (let [{:keys [name pin]} @form
            valid? (and (not (str/blank? name))
                        (re-matches #"\d{4}" (or pin "")))]
        [:div.pin-modal-overlay
         [:div.pin-modal {:style {:padding-top "1.5rem"}}
          [:h2.pin-modal-title "Willkommen"]
          [:p {:style {:padding "0 2rem"}} "Richte den ersten Admin-Account ein, um loszulegen."]
          [:div.form-field
           [:label "Name"]
           [:input.input {:type        "text"
                          :placeholder "z.B. Alex Bauer"
                          :value       name
                          :on-change   #(swap! form assoc :name (.. % -target -value))}]]
          [:div.form-field
           [:label "PIN (4 Ziffern)"]
           [:input.input {:type        "password"
                          :input-mode  "numeric"
                          :placeholder "••••"
                          :max-length  4
                          :value       pin
                          :on-change   (fn [e]
                                         (let [v (str/replace (.. e -target -value) #"\D" "")]
                                           (when (<= (count v) 4)
                                             (swap! form assoc :pin v))))}]]
          [:button.button.is-primary.is-fullwidth
           {:disabled (not valid?)
            :on-click (fn []
                        (re-frame/dispatch [:command/create-user
                                            {:name     name
                                             :role     :admin
                                             :pin-hash (user-events/hash-pin pin)}]))}
           "Einrichten"]]]))))

(defn home-page []
  (let [all-users    (re-frame/subscribe [::user-subs/all-users])
        pin-state    (re-frame/subscribe [::auth-subs/pin-state])
        rfid-toast   (re-frame/subscribe [::auth-subs/rfid-toast])
        search-query (r/atom "")]
    (fn []
      (let [{:keys [user] :as pin} @pin-state
            toast        @rfid-toast
            users        @all-users
            q            (str/lower-case (or @search-query ""))
            filtered     (if (str/blank? q)
                           users
                           (filter #(str/includes? (str/lower-case (:user/name %)) q) users))
            by-letter    (sort-by first (group-by #(first (:user/name %)) filtered))
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
            (:message toast)])
         (when (empty? users)
           [onboarding-modal])]))))
