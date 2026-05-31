(ns feuerwehr-strichliste.pages.activity
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.views :refer [format-price]]))

(defn- format-date [iso]
  (.toLocaleString (js/Date. iso) "de-DE"
                   #js {:day "2-digit" :month "2-digit" :year "2-digit"
                        :hour "2-digit" :minute "2-digit"}))

(defn- event-type-label [event-type]
  (case event-type
    :user/created             "Nutzer erstellt"
    :user/updated             "Nutzer aktualisiert"
    :item/created             "Artikel erstellt"
    :item/edited              "Artikel bearbeitet"
    :item/restocked           "Artikel aufgefüllt"
    :item/stock-corrected     "Inventur"
    :cart/checked-out         "Einkauf"
    :balance/top-up-requested "Einzahlung beantragt"
    :balance/top-up-confirmed "Einzahlung bestätigt"
    :balance/top-up-cancelled "Einzahlung storniert"
    :auth/sign-in-attempted   "Anmeldeversuch"
    :auth/signed-out          "Abgemeldet"
    (name event-type)))

(defn- details-cell [event users-map items-map event-log]
  (case (:event/type event)
    :cart/checked-out
    (let [entries (:checkout/entries event)
          total   (reduce (fn [s {:keys [quantity unit-price]}] (+ s (* quantity unit-price))) 0 entries)]
      [:td.is-size-7.has-text-grey
       (for [{:keys [item-id quantity]} entries]
         ^{:key item-id}
         [:span (str quantity "× " (get-in items-map [item-id :item/name] "?")) " "])
       [:span.has-text-weight-semibold (str "−" (format-price total))]])

    :balance/top-up-requested
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (format-price (:top-up/amount event))]
     (str " für " (get-in users-map [(:event/subject event) :user/name] "?"))]

    :balance/top-up-cancelled
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (format-price (:top-up/amount event))]
     (str " für " (get-in users-map [(:event/subject event) :user/name] "?"))]

    :balance/top-up-confirmed
    (let [req (first (filter #(= (:event/id %) (:top-up/request-id event)) event-log))]
      [:td.is-size-7.has-text-grey
       (when req
         [:<>
          [:span.has-text-weight-semibold (format-price (:top-up/amount req))]
          (str " für " (get-in users-map [(:event/subject req) :user/name] "?"))])])

    :item/restocked
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (str "+" (:item/stock event))]
     (str " · " (get-in items-map [(:item/id event) :item/name] "?"))]

    :item/stock-corrected
    [:td.is-size-7.has-text-grey
     (str (get-in items-map [(:item/id event) :item/name] "?") " ")
     [:span.has-text-weight-semibold (str (:item/stock-before event) " → " (:item/stock event))]]

    :item/created  [:td.is-size-7.has-text-grey (:item/name event)]
    :item/edited   [:td.is-size-7.has-text-grey (:item/name event)]
    :user/created  [:td.is-size-7.has-text-grey (:user/name event)]
    :user/updated  [:td.is-size-7.has-text-grey (:user/name event)]

    :auth/sign-in-attempted
    [:td
     (if (:auth/success event)
       [:span.tag.is-success.is-light "Erfolg"]
       [:span.tag.is-danger.is-light "Fehlschlag"])]

    [:td]))

(defn- event-row [event users-map items-map event-log]
  (let [actor-id   (:event/actor event)
        subject-id (:event/subject event)
        actor-name (get-in users-map [actor-id :user/name] "?")
        nutzer     (if (and subject-id (not= subject-id actor-id))
                     (str (get-in users-map [subject-id :user/name] "?")
                          " (durch " actor-name ")")
                     actor-name)]
    [:tr
     [:td.is-size-7.has-text-grey (format-date (:event/timestamp event))]
     [:td (event-type-label (:event/type event))]
     [:td nutzer]
     [details-cell event users-map items-map event-log]]))

(defn activity-page []
  (let [event-log (re-frame/subscribe [::app-subs/event-log])
        users-map (re-frame/subscribe [::user-subs/users-map])
        items-map (re-frame/subscribe [::item-subs/items-map])]
    (fn []
      [:div
       [:nav.top-nav
        [:button.button.is-ghost
         {:on-click #(re-frame/dispatch [::events/navigate :overview])}
         [:span.icon [:i.fas.fa-arrow-left]]
         [:span "Zurück"]]
        [:span.top-nav-name "Aktivitätslog"]
        [:span]]

       [:div {:style {:padding "1.5rem"}}
        (if (empty? @event-log)
          [:p.has-text-grey "Keine Aktivitäten vorhanden."]
          [:div {:style {:background    "var(--color-surface)"
                         :border        "1px solid var(--color-outline)"
                         :border-radius "var(--radius)"
                         :box-shadow    "var(--shadow)"
                         :overflow      "hidden"}}
           [:div.table-container
            [:table.table.is-fullwidth.is-hoverable
             [:thead
              [:tr
               [:th "Zeit"]
               [:th "Ereignis"]
               [:th "Nutzer"]
               [:th "Details"]]]
             [:tbody
              (let [log @event-log
                    um  @users-map
                    im  @items-map]
                (for [event (reverse log)]
                  ^{:key (:event/id event)}
                  [event-row event um im log]))]]]])]])))
