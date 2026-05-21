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
   [feuerwehr-strichliste.top-up.subs :as top-up-subs]
   [feuerwehr-strichliste.top-up.events :as top-up-events]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]
   [feuerwehr-strichliste.item.views :refer [new-item-form edit-item-form item-card format-price]]
   [feuerwehr-strichliste.user.views :refer [edit-user-form]]
   [feuerwehr-strichliste.top-up.views :refer [top-up-form]]))

(defn- format-balance [cents]
  (if (neg? cents)
    (str "-" (format-price (- cents)))
    (format-price cents)))

(defn- balance-class [cents]
  (cond (pos? cents) "positive" (neg? cents) "negative" :else "zero"))

(defn- session-pane [balance cart-entries pending-top-up top-up-editing? user]
  (let [cart-sum  (reduce (fn [s {:keys [item quantity]}] (+ s (* quantity (:item/price item)))) 0 cart-entries)
        tu-sum    (or (:amount pending-top-up) 0)
        projected (- (+ balance tu-sum) cart-sum)]
    [:div.session-pane
     [:div.session-pane-balance
      [:div.session-pane-balance-label "Guthaben"]
      [:span.session-pane-balance-amount {:class (balance-class projected)}
       (format-balance projected)]
      (if top-up-editing?
        [top-up-form {:current-user   user
                      :initial-amount (:amount pending-top-up)
                      :on-close       #(re-frame/dispatch [::top-up-events/close-top-up-form])}]
        [:button.button.is-light.is-fullwidth
         {:on-click #(re-frame/dispatch [::top-up-events/open-top-up-form])}
         [:span.icon.is-small [:i.fas.fa-coins]]
         [:span "Einzahlen"]])]
     [:div.session-pane-entries
      (if (and (empty? cart-entries) (nil? pending-top-up))
        [:div.session-pane-empty "Noch nichts ausgewählt"]
        [:<>
         (for [{:keys [item quantity]} cart-entries]
           ^{:key (:item/id item)}
           [:div.session-entry
            [:span.session-entry-name (:item/name item)]
            [:span.session-entry-qty (str "× " quantity)]
            [:span.session-entry-price (format-price (* quantity (:item/price item)))]
            [:button.session-entry-action
             {:on-click #(re-frame/dispatch [::item-events/decrement (:item/id item)])}
             [:span.icon.is-small [:i.fas.fa-minus]]]])
         (when pending-top-up
           [:div.session-entry
            [:span.session-entry-name "Einzahlung"]
            [:span.session-entry-qty ""]
            [:span.session-entry-price {:style {:color "green"}}
             (str "+ " (format-price (:amount pending-top-up)))]
            [:button.session-entry-action
             {:on-click #(re-frame/dispatch [::top-up-events/open-top-up-form])}
             [:span.icon.is-small [:i.fas.fa-pen]]]
            [:button.session-entry-action
             {:on-click #(re-frame/dispatch [::top-up-events/clear-staged-top-up])}
             [:span.icon.is-small [:i.fas.fa-times]]]])])]
     [:div.session-pane-footer
      [:button.button.is-primary.is-fullwidth
       {:on-click #(do (re-frame/dispatch [::item-events/confirm-checkout])
                       (re-frame/dispatch [::auth-events/sign-out]))}
       "Fertig"]]]))

(defn overview-page []
  (let [current-user    (re-frame/subscribe [::app-subs/current-user])
        balance         (re-frame/subscribe [::app-subs/current-user-balance])
        active-tab      (re-frame/subscribe [::item-subs/active-tab])
        items-by-type   (re-frame/subscribe [::item-subs/items-by-type])
        cart-entries    (re-frame/subscribe [::item-subs/cart-entries])
        editing-item    (re-frame/subscribe [::item-subs/editing-item])
        can-manage?     (re-frame/subscribe [::item-subs/can-manage-items?])
        profile-open?   (re-frame/subscribe [::user-subs/profile-open?])
        pending-top-up  (re-frame/subscribe [::top-up-subs/pending-top-up])
        top-up-editing? (re-frame/subscribe [::top-up-subs/top-up-editing?])
        pane-open?      (r/atom true)
        drawer-open?    (r/atom false)]
    (fn []
      (let [user  @current-user
            bal   @balance
            tab   @active-tab
            admin? (= :admin (:user/role user))
            pane? @pane-open?]
        [:div.overview-layout
         [:div.main-content
          [:nav.top-nav
           [:div.top-nav-left
            [:div.top-nav-identity
             [:span.top-nav-name (:user/name user)]
             [:button.button.is-ghost.is-small
              {:on-click #(re-frame/dispatch [::user-events/open-profile])}
              [:span.icon.is-small [:i.fas.fa-pen]]]]
            [:button.button.is-light.is-small
             {:on-click #(re-frame/dispatch [::events/navigate :history])}
             [:span.icon.is-small [:i.fas.fa-history]]
             [:span "Verlauf"]]
            (when admin?
              [:div.top-nav-admin-actions
               [:button.button.is-light.is-small
                {:on-click #(re-frame/dispatch [::events/navigate :users])}
                [:span.icon.is-small [:i.fas.fa-users]]
                [:span "Nutzer"]]
               [:button.button.is-light.is-small
                {:on-click #(re-frame/dispatch [::events/navigate :top-ups])}
                [:span.icon.is-small [:i.fas.fa-coins]]
                [:span "Einzahlungen"]]
               [:button.button.is-light.is-small
                {:on-click #(re-frame/dispatch [::events/navigate :activity])}
                [:span.icon.is-small [:i.fas.fa-list]]
                [:span "Aktivität"]]])]
           [:div.top-nav-right
            (when-not pane?
              [:div.top-nav-balance-area
               [:span.top-nav-balance {:class (balance-class bal)}
                (format-balance bal)]])
            [:button.button.is-light.is-small
             {:on-click #(swap! pane-open? not)}
             [:span.icon.is-small [:i.fas.fa-columns]]]]]
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
           (concat
            (when @can-manage?
              [^{:key "new-item"}
               [:button.item-card
                {:on-click #(reset! drawer-open? true)
                 :style    {:background      "#4CAF50"
                             :border-color    "#4CAF50"
                             :color           "#fff"
                             :cursor          "pointer"
                             :justify-content "center"
                             :align-items     "center"}}
                [:span.icon.is-large [:i.fas.fa-plus]]
                [:span {:style {:font-size "0.9rem" :font-weight 600}} "Neu"]]])
            (for [item (get @items-by-type tab [])]
              ^{:key (:item/id item)} [item-card item]))]]
         (when pane?
           [session-pane bal @cart-entries @pending-top-up @top-up-editing? user])
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
             #(re-frame/dispatch [::user-events/close-profile])])]]))))
