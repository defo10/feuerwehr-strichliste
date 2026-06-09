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

(defn- export-pdf! [top-ups users-map]
  (let [confirmed (filter #(= :confirmed (:top-up/status %)) top-ups)
        today     (.toLocaleDateString (js/Date.) "de-DE")
        rows      (apply str
                         (map (fn [t]
                                (str "<tr>"
                                     "<td>" (format-date (:top-up/requested-at t)) "</td>"
                                     "<td>" (get-in users-map [(:top-up/user-id t) :user/name] "?") "</td>"
                                     "<td>Einzahlung</td>"
                                     "<td>" (format-price (:top-up/amount t)) "</td>"
                                     "<td>" (get-in users-map [(:top-up/reviewed-by t) :user/name] "?") "</td>"
                                     "<td></td>"
                                     "</tr>"))
                              confirmed))
        html      (str "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                       "<style>"
                       "body{font-family:sans-serif;font-size:13px;padding:2rem}"
                       "h2{margin-bottom:0.25rem}"
                       "p{margin-bottom:1rem;color:#666;font-size:12px}"
                       "table{width:100%;border-collapse:collapse}"
                       "th,td{border:1px solid #ccc;padding:0.4rem 0.6rem;text-align:left}"
                       "th{background:#f5f5f5;font-weight:600}"
                       "td:last-child{width:8rem}"
                       "</style></head><body>"
                       "<h2>Einzahlungen</h2>"
                       "<p>Exportiert am " today "</p>"
                       "<table><thead><tr>"
                       "<th>Datum</th><th>Empfänger</th><th>Umsatzart</th>"
                       "<th>Höhe</th><th>Bestätigt von</th><th>Unterschrift</th>"
                       "</tr></thead><tbody>" rows "</tbody></table>"
                       "</body></html>")
        iframe    (.createElement js/document "iframe")]
    (set! (.. iframe -style -display) "none")
    (.appendChild js/document.body iframe)
    (.write (.. iframe -contentWindow -document) html)
    (.close (.. iframe -contentWindow -document))
    (.addEventListener (.-contentWindow iframe) "afterprint"
                       (fn [] (.remove iframe)))
    (.print (.-contentWindow iframe))))

(defn- status-cell [top-up users-map]
  (let [status       (:top-up/status top-up)
        reviewed-by (get-in users-map [(:top-up/reviewed-by top-up) :user/name])]
    (case status
      :pending
      [:td
       [:div.buttons.are-small
        [:button.button.is-success.is-small
         {:on-click #(re-frame/dispatch [::top-up-events/confirm-top-up
                                         {:request-id (:top-up/id top-up)
                                          :user-id    (:top-up/user-id top-up)}])}
         "Bestätigen"]
        [:button.button.is-light.is-small
         {:on-click #(re-frame/dispatch [::top-up-events/cancel-top-up
                                         {:request-id (:top-up/id top-up)
                                          :user-id    (:top-up/user-id top-up)}])}
         "Stornieren"]]]
      :confirmed
      [:td
       [:span.tag.is-success "Bestätigt"]
       [:br]
       [:span.is-size-7.has-text-grey
        (str reviewed-by " · " (format-date (:top-up/reviewed-at top-up)))]]
      :cancelled
      [:td
       [:span.tag "Storniert"]
       [:br]
       [:span.is-size-7.has-text-grey
        (str reviewed-by " · " (format-date (:top-up/reviewed-at top-up)))]])))

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
          [:button.button.is-light.is-small
           {:on-click #(export-pdf! top-ups users-map)
            :disabled (not (some #(= :confirmed (:top-up/status %)) top-ups))}
           [:span.icon.is-small [:i.fas.fa-file-export]]
           [:span "Exportieren"]]]

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
