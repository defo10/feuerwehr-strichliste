(ns feuerwehr-strichliste.overview.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.overview.subs :as subs]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]
   [feuerwehr-strichliste.overview.new-item-form :refer [new-item-form]]))

(defn- format-price [cents]
  (str (quot cents 100) "," (let [r (mod cents 100)] (if (< r 10) (str "0" r) r)) " €"))

(defn- item-card [{:item/keys [name price stock type]}]
  [:div.item-card
   [:div.item-card-name name]
   [:div.item-card-price (format-price price)]
   [:div.item-card-meta
    [:span.item-card-type (if (= :food type) "Essen" "Trinken")]
    [:span.item-card-stock (str "Vorrat: " stock)]]])

(defn- actions-for [role open-new-item!]
  (let [kitchen [{:icon "➕" :color "#4CAF50" :title "Neues Essen/Trinken hinzufügen" :on-click open-new-item!}
                 {:icon "✏️"  :color "#2196F3" :title "Essen/Trinken bearbeiten"}
                 {:icon "📦" :color "#FF9800" :title "Bestand bearbeiten"}]
        admin   [{:icon "👥" :color "#9C27B0" :title "Nutzer verwalten"}
                 {:icon "💰" :color "#009688" :title "Einzahlungen überblicken"}]]
    (case role
      :kitchen kitchen
      :admin   (concat kitchen admin)
      nil)))

(defn- action-button [{:keys [icon color title on-click]}]
  [:button.action-button {:style {:background color} :on-click on-click}
   [:span.action-icon icon]
   [:span.action-title title]])

(defn- action-bar [actions]
  [:div.action-bar
   (for [{:keys [title] :as action} actions]
     ^{:key title} [action-button action])])

(defn overview-page []
  (let [current-user (re-frame/subscribe [::app-subs/current-user])
        items        (re-frame/subscribe [::subs/items])
        drawer-open? (r/atom false)]
    (fn []
      (let [user    @current-user
            actions (actions-for (:user/role user) #(reset! drawer-open? true))]
        [:div
         [:nav.top-nav
          [:span.top-nav-name (:user/name user)]
          [:button.top-nav-logout
           {:on-click #(re-frame/dispatch [::events/sign-out])}
           "Fertig"]]
         (when (seq actions)
           [action-bar actions])
         [:div.item-grid
          (for [item @items]
            ^{:key (:item/id item)} [item-card item])]
         [drawer {:open?    @drawer-open?
                  :on-close #(reset! drawer-open? false)
                  :title    "Neues Essen/Trinken"}
          [new-item-form #(reset! drawer-open? false)]]]))))
