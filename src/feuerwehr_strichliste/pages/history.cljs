(ns feuerwehr-strichliste.pages.history
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.views :refer [format-price]]))

(defn- format-timestamp [ts]
  (.toLocaleString (js/Date. ts) "de-DE"
                   #js {:weekday "long"
                        :day     "numeric"
                        :month   "long"
                        :year    "numeric"
                        :hour    "2-digit"
                        :minute  "2-digit"}))

(def ^:private th-style {:font-weight 500
                          :color       "var(--color-on-surface-muted)"
                          :font-size   "0.75rem"
                          :padding     "0 0 0.3rem 0"
                          :white-space "nowrap"})

(defn- format-net [cents]
  (cond
    (pos? cents) (str "+ " (format-price cents))
    (neg? cents) (str "- " (format-price (- cents)))
    :else        (format-price cents)))

(defn- net-color [cents]
  (cond
    (pos? cents) "#22863a"
    (neg? cents) "var(--color-on-surface)"
    :else        "var(--color-on-surface-muted)"))

(defn- session-card [events items-map]
  (let [checkout  (some #(when (= :cart/checked-out (:event/type %)) %) events)
        top-up    (some #(when (= :balance/top-up-requested (:event/type %)) %) events)
        ts        (:event/timestamp (first events))
        entries   (:checkout/entries checkout)
        order-total (reduce (fn [s {:keys [quantity unit-price]}]
                              (+ s (* quantity unit-price)))
                            0 entries)
        net       (- (or (:top-up/amount top-up) 0) order-total)]
    [:div {:style {:background    "var(--color-surface)"
                   :border        "1px solid var(--color-outline)"
                   :border-radius "var(--radius)"
                   :box-shadow    "var(--shadow)"
                   :margin-bottom "1rem"
                   :padding       "1.25rem"}}
     [:div {:style {:display         "flex"
                    :justify-content "space-between"
                    :align-items     "baseline"
                    :margin-bottom   "0.75rem"
                    :padding-bottom  "0.5rem"
                    :border-bottom   "1px solid var(--color-outline)"}}
      [:span {:style {:font-size "0.875rem" :color "var(--color-on-surface-muted)"}}
       (format-timestamp ts)]
      [:strong {:style {:color (net-color net)}} (format-net net)]]
     (if checkout
       [:table {:style {:width "100%" :border-collapse "collapse" :font-size "0.875rem"}}
        [:thead
         [:tr
          [:th {:style (assoc th-style :text-align "left")} "Artikel"]
          [:th {:style (assoc th-style :text-align "right" :padding-left "1rem")} "Menge"]
          [:th {:style (assoc th-style :text-align "right" :padding-left "1rem")} "Preis/Stk."]
          [:th {:style (assoc th-style :text-align "right" :padding-left "1rem")} "Gesamt"]]]
        [:tbody
         (for [{:keys [item-id quantity unit-price]} entries]
           ^{:key item-id}
           [:tr
            [:td {:style {:padding "0.2rem 0"}}
             (or (get-in items-map [item-id :item/name]) (str item-id))]
            [:td {:style {:text-align "right" :padding "0.2rem 0 0.2rem 1rem"}} quantity]
            [:td {:style {:text-align "right" :padding "0.2rem 0 0.2rem 1rem"}} (format-price unit-price)]
            [:td {:style {:text-align "right" :padding "0.2rem 0 0.2rem 1rem"}}
             (format-price (* quantity unit-price))]])
         (when top-up
           [:tr {:style {:border-top "1px solid var(--color-outline)"
                         :color      "#22863a"}}
            [:td {:colSpan 3 :style {:padding "0.3rem 0"}} "Einzahlung"]
            [:td {:style {:text-align "right" :padding "0.3rem 0 0.3rem 1rem"}}
             (str "+ " (format-price (:top-up/amount top-up)))]])]]
       [:div {:style {:display "flex" :justify-content "space-between" :font-size "0.875rem"}}
        [:span "Einzahlung"]
        [:strong {:style {:color "#22863a"}}
         (str "+ " (format-price (:top-up/amount top-up)))]])]))

(defn history-page []
  (let [history   (re-frame/subscribe [::app-subs/user-history])
        items-map (re-frame/subscribe [::item-subs/items-map])]
    (fn []
      [:<>
       [:nav.top-nav
        [:button.button.is-ghost
         {:on-click #(re-frame/dispatch [::events/navigate :overview])}
         [:span.icon [:i.fas.fa-arrow-left]]
         [:span "Zurück"]]
        [:span.top-nav-name {:style {:position  "absolute"
                                    :left      "50%"
                                    :transform "translateX(-50%)"}} "Verlauf"]]
       [:div {:style {:padding "1.5rem"}}
        (if (empty? @history)
          [:p {:style {:color "var(--color-on-surface-muted)"}} "Noch keine Aktivität"]
          (for [group @history]
            ^{:key (:event/timestamp (first group))}
            [session-card group @items-map]))]])))
