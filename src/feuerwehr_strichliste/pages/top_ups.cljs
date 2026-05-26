(ns feuerwehr-strichliste.pages.top-ups
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.top-up.subs :as top-up-subs]
   [feuerwehr-strichliste.top-up.events :as top-up-events]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.item.views :refer [format-price]]))

(defn- format-date [iso]
  (.toLocaleString (js/Date. iso) "de-DE"
                   #js {:day "2-digit" :month "2-digit" :year "2-digit"
                        :hour "2-digit" :minute "2-digit"}))

(defn- status-cell [top-up users-map]
  (let [status       (:top-up/status top-up)
        confirmed-by (get-in users-map [(:top-up/confirmed-by top-up) :user/name])
        cancelled-by (get-in users-map [(:top-up/cancelled-by top-up) :user/name])]
    (case status
      :pending
      [:td
       [:div.buttons.are-small
        [:button.button.is-success.is-small
         {:on-click #(re-frame/dispatch [::top-up-events/confirm-top-up (:top-up/id top-up)])}
         "Bestätigen"]
        [:button.button.is-light.is-small
         {:on-click #(re-frame/dispatch [::top-up-events/cancel-top-up (:top-up/id top-up)])}
         "Stornieren"]]]
      :confirmed
      [:td
       [:span.tag.is-success "Bestätigt"]
       [:br]
       [:span.is-size-7.has-text-grey
        (str confirmed-by " · " (format-date (:top-up/confirmed-at top-up)))]]
      :cancelled
      [:td
       [:span.tag "Storniert"]
       [:br]
       [:span.is-size-7.has-text-grey
        (str cancelled-by " · " (format-date (:top-up/cancelled-at top-up)))]])))

(defn- top-up-row [top-up users-map]
  (let [status    (:top-up/status top-up)
        user-name (get-in users-map [(:top-up/user-id top-up) :user/name] "?")]
    [:tr {:class (when (= status :cancelled) "has-text-grey")}
     [:td user-name]
     [:td.has-text-weight-semibold (format-price (:top-up/amount top-up))]
     [:td.is-size-7.has-text-grey (format-date (:top-up/requested-at top-up))]
     [status-cell top-up users-map]]))

(defn top-ups-page []
  (let [top-ups   (re-frame/subscribe [::top-up-subs/top-ups-sorted])
        users-map (re-frame/subscribe [::user-subs/users-map])]
    (fn []
      (let [top-ups   @top-ups
            users-map @users-map]
        [:div
         [:nav.top-nav
          [:button.button.is-ghost
           {:on-click #(re-frame/dispatch [::events/navigate :overview])}
           [:span.icon [:i.fas.fa-arrow-left]]
           [:span "Zurück"]]
          [:span.top-nav-name "Einzahlungen"]
          [:span]]

         [:div {:style {:padding "1.5rem"}}
          (if (empty? top-ups)
            [:p.has-text-grey "Keine Einzahlungen vorhanden."]
            [:div {:style {:background    "var(--color-surface)"
                           :border        "1px solid var(--color-outline)"
                           :border-radius "var(--radius)"
                           :box-shadow    "var(--shadow)"
                           :overflow      "hidden"}}
             [:div.table-container
              [:table.table.is-fullwidth.is-hoverable
               [:thead
                [:tr
                 [:th "Nutzer"]
                 [:th "Betrag"]
                 [:th "Eingereicht"]
                 [:th "Status"]]]
               [:tbody
                (for [top-up top-ups]
                  ^{:key (:top-up/id top-up)}
                  [top-up-row top-up users-map])]]]])]]))))
