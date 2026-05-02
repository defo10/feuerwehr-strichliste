(ns feuerwehr-strichliste.pages.overview
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.auth.events :as auth-events]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.events :as item-events]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.user.events :as user-events]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]
   [feuerwehr-strichliste.item.views :refer [new-item-form edit-item-form item-card receipt-overlay format-price]]
   [feuerwehr-strichliste.user.views :refer [edit-user-form]]
   [feuerwehr-strichliste.top-up.views :refer [top-up-form]]))

(defn- format-balance [cents]
  (if (neg? cents)
    (str "-" (format-price (- cents)))
    (format-price cents)))

(defn- balance-class [cents]
  (cond (pos? cents) "positive" (neg? cents) "negative" :else "zero"))

(defn- actions-for [role open-new-item!]
  (let [kitchen [{:icon "➕" :color "#4CAF50" :title "Neues Essen/Trinken hinzufügen" :on-click open-new-item!}
                 {:icon "✏️"  :color "#2196F3" :title "Essen/Trinken bearbeiten"}
                 {:icon "📦" :color "#FF9800" :title "Bestand bearbeiten"}]
        admin   [{:icon "👥" :color "#9C27B0" :title "Nutzer verwalten"
                  :on-click #(re-frame/dispatch [::events/navigate :users])}
                 {:icon "💰" :color "#3F51B5" :title "Einzahlungen überblicken"
                  :on-click #(re-frame/dispatch [::events/navigate :top-ups])}]]
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
        balance       (re-frame/subscribe [::app-subs/current-user-balance])
        active-tab    (re-frame/subscribe [::item-subs/active-tab])
        items-by-type (re-frame/subscribe [::item-subs/items-by-type])
        has-items?    (re-frame/subscribe [::item-subs/cart-has-items?])
        cart-total    (re-frame/subscribe [::item-subs/cart-total])
        receipt       (re-frame/subscribe [::item-subs/receipt])
        editing-item  (re-frame/subscribe [::item-subs/editing-item])
        profile-open? (re-frame/subscribe [::user-subs/profile-open?])
        all-users     (re-frame/subscribe [::user-subs/all-users])
        drawer-open?  (r/atom false)
        top-up-open?  (r/atom false)]
    (fn []
      (let [user      @current-user
            bal       @balance
            cart      @cart-total
            has-cart? @has-items?
            projected (- bal cart)
            tab       @active-tab
            actions   (actions-for (:user/role user) #(reset! drawer-open? true))]
        [:<>
         [:div
          [:nav.top-nav
           [:div.top-nav-identity
            [:span.top-nav-name (:user/name user)]
            [:button.top-nav-edit-profile
             {:on-click #(re-frame/dispatch [::user-events/open-profile])}
             "BEARBEITEN"]]
           [:div.top-nav-balance-area
            [:span.top-nav-balance {:class (balance-class (if has-cart? projected bal))}
             (format-balance (if has-cart? projected bal))
             (when has-cart?
               [:span.top-nav-balance-delta (str " (-" (format-price cart) ")")])]
            [:button.top-nav-top-up-btn {:on-click #(reset! top-up-open? true)}
             "Geld einzahlen"]]
           [:button.top-nav-logout
            {:on-click #(if has-cart?
                          (re-frame/dispatch [::item-events/show-receipt])
                          (re-frame/dispatch [::auth-events/sign-out]))}
            "Fertig"]]
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
             #(re-frame/dispatch [::item-events/close-edit])])]
         [drawer {:open?    @profile-open?
                  :on-close #(re-frame/dispatch [::user-events/close-profile])
                  :title    "Mein Profil"}
          (when @profile-open?
            [edit-user-form user :self
             #(re-frame/dispatch [::user-events/close-profile])])]
         [drawer {:open?    @top-up-open?
                  :on-close #(reset! top-up-open? false)
                  :title    "Einzahlung melden"}
          (when @top-up-open?
            [top-up-form {:current-user user
                          :all-users    @all-users
                          :on-close     #(reset! top-up-open? false)}])]]))))
