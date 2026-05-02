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

(defn- top-up-row [top-up users-map]
  (let [status       (:top-up/status top-up)
        user-name    (get-in users-map [(:top-up/user-id top-up) :user/name] "?")
        confirmed-by (get-in users-map [(:top-up/confirmed-by top-up) :user/name])
        cancelled-by (get-in users-map [(:top-up/cancelled-by top-up) :user/name])]
    [:div.data-table-row {:class (name status)}
     [:span.data-table-cell.top-ups-table-name user-name]
     [:span.data-table-cell.top-ups-table-amount (format-price (:top-up/amount top-up))]
     [:span.data-table-cell.top-ups-table-date (format-date (:top-up/requested-at top-up))]
     (case status
       :pending
       [:div.data-table-cell.top-ups-table-actions
        [:button.top-up-btn.confirm
         {:on-click #(re-frame/dispatch [::top-up-events/confirm-top-up (:top-up/id top-up)])}
         "Bestätigen"]
        [:button.top-up-btn.cancel
         {:on-click #(re-frame/dispatch [::top-up-events/cancel-top-up (:top-up/id top-up)])}
         "Stornieren"]]
       :confirmed
       [:div.data-table-cell.top-up-status-label
        [:span "Bestätigt"]
        [:span.top-up-status-when (str confirmed-by " · " (format-date (:top-up/confirmed-at top-up)))]]
       :cancelled
       [:div.data-table-cell.top-up-status-label.muted
        [:span "Storniert"]
        [:span.top-up-status-when (str cancelled-by " · " (format-date (:top-up/cancelled-at top-up)))]])]))

(defn top-ups-page []
  (let [top-ups   (re-frame/subscribe [::top-up-subs/top-ups-sorted])
        users-map (re-frame/subscribe [::user-subs/users-map])]
    (fn []
      [:div
       [:nav.top-nav {:style {:display "grid" :grid-template-columns "1fr auto 1fr" :align-items "center"}}
        [:button.top-nav-back
         {:on-click #(re-frame/dispatch [::events/navigate :overview])
          :style    {:justify-self "start"}}
         "←"]
        [:span.top-nav-name "Einzahlungen"]
        [:span]]

       [:div.top-ups-table
        [:div.data-table-header
         [:span "Nutzer"]
         [:span "Betrag"]
         [:span "Eingereicht"]
         [:span "Status"]]
        (if (empty? @top-ups)
          [:div.top-ups-empty {:style {:grid-column "1 / -1"}} "Keine Einzahlungen vorhanden."]
          (for [top-up @top-ups]
            ^{:key (:top-up/id top-up)}
            [top-up-row top-up @users-map]))]])))
