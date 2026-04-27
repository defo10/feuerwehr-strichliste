(ns feuerwehr-strichliste.pages.overview
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.auth.events :as auth-events]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.events :as item-events]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]
   [feuerwehr-strichliste.item.views :refer [new-item-form edit-item-form item-card receipt-overlay format-price]]))

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
  (let [current-user  (re-frame/subscribe [::app-subs/current-user])
        active-tab    (re-frame/subscribe [::item-subs/active-tab])
        items-by-type (re-frame/subscribe [::item-subs/items-by-type])
        has-items?    (re-frame/subscribe [::item-subs/cart-has-items?])
        cart-total    (re-frame/subscribe [::item-subs/cart-total])
        receipt       (re-frame/subscribe [::item-subs/receipt])
        editing-item  (re-frame/subscribe [::item-subs/editing-item])
        drawer-open?  (r/atom false)]
    (fn []
      (let [user    @current-user
            tab     @active-tab
            actions (actions-for (:user/role user) #(reset! drawer-open? true))]
        [:<>
         [:div
          [:nav.top-nav
           [:span.top-nav-name (:user/name user)]
           [:button.top-nav-logout
            {:on-click #(if @has-items?
                          (re-frame/dispatch [::item-events/show-receipt])
                          (re-frame/dispatch [::auth-events/sign-out]))}
            (if @has-items?
              (str "Fertig (" (format-price @cart-total) ")")
              "Fertig")]]
          (when (seq actions)
            [action-bar actions])
          [:div.tab-bar
           [:button.tab
            {:class    (when (= tab :drink) "tab--active")
             :on-click #(re-frame/dispatch [::item-events/set-active-tab :drink])}
            "Getränke"]
           [:button.tab
            {:class    (when (= tab :food) "tab--active")
             :on-click #(re-frame/dispatch [::item-events/set-active-tab :food])}
            "Essen"]]
          [:div.item-grid
           (for [item (get @items-by-type tab [])]
             ^{:key (:item/id item)} [item-card item])]]
         (when @receipt
           [receipt-overlay @receipt])
         [drawer {:open?    @drawer-open?
                  :on-close #(reset! drawer-open? false)
                  :title    "Neues Essen/Trinken"}
          [new-item-form #(reset! drawer-open? false)]]
         [drawer {:open?    (some? @editing-item)
                  :on-close #(re-frame/dispatch [::item-events/close-edit])
                  :title    "Essen/Trinken bearbeiten"}
          (when @editing-item
            [edit-item-form @editing-item
             #(re-frame/dispatch [::item-events/close-edit])])]]))))
