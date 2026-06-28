(ns feuerwehr-strichliste.pages.users
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.user.events :as user-events]
   [feuerwehr-strichliste.user.views :refer [new-user-form edit-user-form role-labels status-labels]]
   [feuerwehr-strichliste.user.search-bar :refer [search-bar]]
   [feuerwehr-strichliste.user.alphabet-bar :refer [alphabet-bar]]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.events :as item-events]
   [feuerwehr-strichliste.item.views :refer [checkout-form format-price]]
   [feuerwehr-strichliste.top-up.events :as top-up-events]
   [feuerwehr-strichliste.top-up.views :refer [admin-top-up-form]]
   [feuerwehr-strichliste.domain.permissions :as permissions]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]))

(def ^:private role-order   {:member 0 :kitchen 1 :admin 2})
(def ^:private status-order {:active 0 :inactive 1 :suspended 2})

(defn- sort-value [col user all-balances]
  (case col
    :name    (some-> (:user/name user) str/lower-case)
    :role    (role-order (:user/role user))
    :status  (status-order (:user/status user))
    :balance (get all-balances (:user/id user) 0)))

(defn- apply-sort [users {:keys [col dir]} all-balances]
  (let [sorted (sort-by #(sort-value col % all-balances) users)]
    (if (= dir :desc) (reverse sorted) sorted)))

(defn- toggle-sort [sort-state col]
  (swap! sort-state (fn [{current-col :col current-dir :dir}]
                      {:col col
                       :dir (if (and (= current-col col) (= current-dir :asc)) :desc :asc)})))

(defn- col-header [sort-state col label]
  (let [{current-col :col dir :dir} @sort-state
        active? (= current-col col)
        arrow   (when active? (if (= dir :asc) " ▲" " ▼"))]
    [:button.col-header {:on-click #(toggle-sort sort-state col)}
     label arrow]))

(defn- role-cell [user]
  [:span.tag (role-labels (:user/role user))])

(defn- status-tag [status]
  (let [label (status-labels status)]
    (case status
      :active    [:span.tag.is-success label]
      :inactive  [:span.tag label]
      :suspended [:span.tag.is-danger label])))

(defn- format-date [iso]
  (.toLocaleString (js/Date. iso) "de-DE"
                   #js {:day "2-digit" :month "2-digit" :year "2-digit"
                        :hour "2-digit" :minute "2-digit"}))

(defn- event-details [event items-map]
  (let [ref    (:checkout/reference event)
        suffix (when (seq ref) (str " (" ref ")"))]
    (case (:history/type event)
      :checkout
      (let [entries (:checkout/entries event)
            total   (reduce (fn [s {:keys [quantity unit-price]}] (+ s (* quantity unit-price))) 0 entries)]
        (str (str/join ", "
                       (map (fn [{:keys [item-id quantity]}]
                              (str quantity "× " (get-in items-map [item-id :item/name] "?")))
                            entries))
             " · −" (format-price total)
             suffix))
      :top-up
      (str (format-price (:top-up/amount event)) suffix)
      "")))

(defn- user-row [user all-balances can-manage? expanded-users anchor-id]
  (let [expanded? (contains? @expanded-users (:user/id user))]
    [:tr (cond-> {:on-click #(swap! expanded-users
                                    (fn [s] (if (contains? s (:user/id user))
                                              (disj s (:user/id user))
                                              (conj s (:user/id user)))))
                  :style {:cursor "pointer"}}
           anchor-id (assoc :id anchor-id
                            :style {:cursor "pointer" :scroll-margin-top "4rem"}))
     [:td (:user/name user)]
     [:td (format-price (get all-balances (:user/id user) 0))]
     [:td [role-cell user]]
     [:td [status-tag (:user/status user)]]
     [:td
      [:span.icon.is-small
       [:i {:class (str "fas " (if expanded? "fa-chevron-up" "fa-chevron-down"))}]]]]))

(defn- user-event-panel [user items-map on-top-up on-checkout on-edit]
  (let [events-sub        (re-frame/subscribe [::subs/user-events-for (:user/id user)])
        users-map-sub     (re-frame/subscribe [::user-subs/users-map])
        current-user-sub  (re-frame/subscribe [::subs/current-user])
        show-all?         (r/atom false)]
    (fn [user items-map on-top-up on-checkout on-edit]
      (let [all-events    @events-sub
            users-map     @users-map-sub
            current-user  @current-user-sub
            can-confirm?  (permissions/can? (:user/role current-user) :confirm-top-ups)
            shown         (if (or @show-all? (<= (count all-events) 30))
                            all-events
                            (take 30 all-events))]
        [:div {:style {:padding "1rem 1.5rem"}}
         [:div.buttons {:style {:margin-bottom "0.75rem"}}
          (when on-edit
            [:button.button.is-small
             {:on-click on-edit}
             [:span.icon.is-small [:i.fas.fa-pencil]]
             [:span "Nutzer bearbeiten"]])
          [:button.button.is-small
           {:on-click on-top-up}
           [:span.icon.is-small [:i.fas.fa-coins]]
           [:span "Einzahlung"]]
          [:button.button.is-small
           {:on-click on-checkout}
           [:span.icon.is-small [:i.fas.fa-shopping-cart]]
           [:span "Einkauf erfassen"]]]
         (if (empty? all-events)
           [:p.is-size-7.has-text-grey "Keine Aktivitäten vorhanden."]
           [:<>
            [:table.table.is-fullwidth.is-narrow.is-size-7
             [:tbody
              (doall
               (for [event shown]
                 (let [status     (:history/status event)
                       cancelled? (#{:voided :cancelled} status)
                       by-admin?  (not= (:history/actor event) (:user/id user))]
                   ^{:key (:history/id event)}
                   [:tr {:class (when cancelled? "has-text-grey")}
                    [:td {:style {:white-space "nowrap"}} (format-date (:history/timestamp event))]
                    [:td {:style {:white-space "nowrap"}}
                     (case (:history/type event)
                       :checkout  "Einkauf"
                       :top-up    "Einzahlung"
                       "")
                     (when by-admin?
                       [:span.tag.is-info.is-light.ml-1 {:style {:font-size "0.65rem" :vertical-align "middle"}}
                        (str "via " (get-in users-map [(:history/actor event) :user/name]))])]
                    [:td (event-details event items-map)]
                    [:td {:style {:white-space "nowrap"}}
                     (let [voidable? (or (= status :active)
                                         (and (= status :confirmed)
                                              (= (:history/actor event) (:user/id current-user))
                                              can-confirm?))]
                       (cond
                         voidable?
                         [:button.button.is-danger.is-outlined.is-small
                          {:on-click #(if (= :checkout (:history/type event))
                                        (re-frame/dispatch [::item-events/void-checkout (:history/id event)])
                                        (re-frame/dispatch [::top-up-events/cancel-top-up {:request-id (:history/id event) :user-id (:user/id user)}]))}
                          "Stornieren"]

                         (#{:voided :cancelled} status)
                         [:span.tag.is-danger.is-light "Storniert"]))]])))]]
            (when (and (not @show-all?) (> (count all-events) 30))
              [:a.is-size-7
               {:on-click #(reset! show-all? true) :style {:cursor "pointer"}}
               "Mehr anzeigen"])])]))))

(defn- users-table [users all-balances can-manage? sort-state expanded-users items-map on-action]
  (let [name-sort? (= :name (:col @sort-state))
        make-row   (fn [user & [anchor-id]]
                     ^{:key (:user/id user)}
                     [:<>
                      [user-row user all-balances can-manage? expanded-users anchor-id]
                      (when (contains? @expanded-users (:user/id user))
                        [:tr.no-hover
                         [:td {:col-span 5}
                          [user-event-panel user items-map
                           #(on-action {:type :top-up   :user user})
                           #(on-action {:type :checkout :user user})
                           (when can-manage? #(re-frame/dispatch [::user-events/edit-user user]))]]])])]
    [:div {:style {:background    "var(--color-surface)"
                   :border        "1px solid var(--color-outline)"
                   :border-radius "var(--radius)"
                   :box-shadow    "var(--shadow)"
                   :overflow      "hidden"}}
     [:div.table-container
      [:table.table.is-fullwidth.is-hoverable
       [:thead
        [:tr
         [:th [col-header sort-state :name "Name"]]
         [:th {:style {:width "1%" :white-space "nowrap"}} [col-header sort-state :balance "Guthaben"]]
         [:th {:style {:width "1%" :white-space "nowrap"}} [col-header sort-state :role "Rolle"]]
         [:th {:style {:width "1%" :white-space "nowrap"}} [col-header sort-state :status "Status"]]
         [:th {:style {:width "1%"}}]]]
       [:tbody
        (if name-sort?
          (doall
           (mapcat
            (fn [group]
              (let [letter    (-> (:user/name (first group)) first str/upper-case)
                    anchor-id (str "users-letter-" letter)]
                (cons (make-row (first group) anchor-id)
                      (map make-row (rest group)))))
            (partition-by #(-> (:user/name %) first str/upper-case) users)))
          (map make-row users))]]]]))

(defn users-page []
  (let [all-users      (re-frame/subscribe [::user-subs/all-users])
        all-balances   (re-frame/subscribe [::subs/all-balances])
        editing-user   (re-frame/subscribe [::user-subs/editing-user])
        can-manage?    (re-frame/subscribe [::user-subs/can-manage-users?])
        items          (re-frame/subscribe [::item-subs/items])
        items-map      (re-frame/subscribe [::item-subs/items-map])
        add-open?      (r/atom false)
        sort-state     (r/atom {:col :name :dir :asc})
        search-query   (r/atom "")
        expanded-users (r/atom #{})
        drawer-state   (r/atom nil)]
    (fn []
      (let [q            (str/lower-case (or @search-query ""))
            filtered     (if (str/blank? q)
                           @all-users
                           (filter #(str/includes? (str/lower-case (:user/name %)) q) @all-users))
            users        (apply-sort filtered @sort-state @all-balances)
            name-sort?   (= :name (:col @sort-state))
            used-letters (when name-sort?
                           (set (map #(-> (:user/name %) first str/upper-case) users)))]
        [:<>
         [:div
          [:nav.top-nav
           [:button.button.is-ghost
            {:on-click #(re-frame/dispatch [::events/navigate :overview])}
            [:span.icon [:i.fas.fa-arrow-left]]
            [:span "Zurück"]]
           [:span.top-nav-name "Nutzer"]
           [search-bar {:value @search-query :on-change #(reset! search-query %)}]
           (if @can-manage?
             [:button.button.is-primary.is-small
              {:on-click #(reset! add-open? true)}
              [:span.icon.is-small [:i.fas.fa-plus]]
              [:span "Hinzufügen"]]
             [:span])]
          [:div {:style {:padding (if name-sort? "1.5rem 3.75rem 1.5rem 1.5rem" "1.5rem")}}
           [users-table users @all-balances @can-manage? sort-state expanded-users @items-map
            #(reset! drawer-state %)]]]

         (when name-sort?
           [alphabet-bar {:used-letters used-letters :id-prefix "users-letter-" :top "4rem"}])

         [drawer {:open?    @add-open?
                  :on-close #(reset! add-open? false)
                  :title    "Neuer Nutzer"}
          [new-user-form #(reset! add-open? false)]]

         [drawer {:open?    (some? @editing-user)
                  :on-close #(re-frame/dispatch [::user-events/close-edit])
                  :title    "Nutzer bearbeiten"}
          (when @editing-user
            [edit-user-form @editing-user :admin
             #(re-frame/dispatch [::user-events/close-edit])])]

         [drawer {:open?    (some? @drawer-state)
                  :on-close #(reset! drawer-state nil)
                  :title    (case (:type @drawer-state)
                              :top-up   "Einzahlung"
                              :checkout "Einkauf erfassen"
                              "")}
          (when-let [{:keys [type user]} @drawer-state]
            (case type
              :top-up   [admin-top-up-form {:user user :on-close #(reset! drawer-state nil)}]
              :checkout [checkout-form {:user user :items @items :on-close #(reset! drawer-state nil)}]
              nil))]]))))
