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
   [feuerwehr-strichliste.components.drawer :refer [drawer]]))

(def ^:private role-order   {:member 0 :kitchen 1 :admin 2})
(def ^:private status-order {:active 0 :inactive 1 :suspended 2})

(defn- sort-value [col user]
  (case col
    :name   (some-> (:user/name user) str/lower-case)
    :role   (role-order (:user/role user))
    :status (status-order (:user/status user))))

(defn- apply-sort [users {:keys [col dir]}]
  (let [sorted (sort-by #(sort-value col %) users)]
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

(defn- role-cell [user can-manage?]
  (if can-manage?
    [:div.select.is-small
     [:select {:value     (name (:user/role user))
               :on-change #(re-frame/dispatch [::user-events/user-update
                                               {:id     (:user/id user)
                                                :name   (:user/name user)
                                                :role   (keyword (.. % -target -value))
                                                :status (:user/status user)}])}
      (for [[k label] role-labels]
        ^{:key k} [:option {:value (name k)} label])]]
    [:span.tag (role-labels (:user/role user))]))

(defn- status-tag [status]
  (let [label (status-labels status)]
    (case status
      :active    [:span.tag.is-success label]
      :inactive  [:span.tag label]
      :suspended [:span.tag.is-danger label])))

(defn- user-row [user all-balances can-manage?]
  [:tr
   [:td (:user/name user)]
   [:td (str (.toFixed (get all-balances (:user/id user) 0) 2) " €")]
   [:td [role-cell user can-manage?]]
   [:td [status-tag (:user/status user)]]
   [:td
    (when can-manage?
      [:button.button.is-ghost.is-small
       {:on-click #(re-frame/dispatch [::user-events/edit-user user])}
       [:span.icon.is-small [:i.fas.fa-pencil]]])]])

(defn- users-table [users all-balances can-manage? sort-state]
  [:div.table-container
   [:table.table.is-fullwidth.is-striped.is-hoverable
    [:thead
     [:tr
      [:th [col-header sort-state :name "Name"]]
      [:th "Guthaben"]
      [:th [col-header sort-state :role "Rolle"]]
      [:th [col-header sort-state :status "Status"]]
      [:th]]]
    [:tbody
     (for [user users]
       ^{:key (:user/id user)}
       [user-row user all-balances can-manage?])]]])

(defn users-page []
  (let [all-users    (re-frame/subscribe [::user-subs/all-users])
        all-balances (re-frame/subscribe [::subs/all-balances])
        editing-user (re-frame/subscribe [::user-subs/editing-user])
        can-manage?  (re-frame/subscribe [::user-subs/can-manage-users?])
        add-open?    (r/atom false)
        sort-state   (r/atom {:col :name :dir :asc})]
    (fn []
      (let [users (apply-sort @all-users @sort-state)]
        [:<>
         [:div
          [:nav.top-nav
           [:button.button.is-ghost
            {:on-click #(re-frame/dispatch [::events/navigate :overview])}
            [:span.icon [:i.fas.fa-arrow-left]]
            [:span "Zurück"]]
           [:span.top-nav-name "Nutzer"]
           (if @can-manage?
             [:button.button.is-primary.is-small
              {:on-click #(reset! add-open? true)}
              [:span.icon.is-small [:i.fas.fa-plus]]
              [:span "Hinzufügen"]]
             [:span])]
          [:div {:style {:padding "1.5rem"}}
           [users-table users @all-balances @can-manage? sort-state]]]

         [drawer {:open?    @add-open?
                  :on-close #(reset! add-open? false)
                  :title    "Neuer Nutzer"}
          [new-user-form #(reset! add-open? false)]]

         [drawer {:open?    (some? @editing-user)
                  :on-close #(re-frame/dispatch [::user-events/close-edit])
                  :title    "Nutzer bearbeiten"}
          (when @editing-user
            [edit-user-form @editing-user :admin
             #(re-frame/dispatch [::user-events/close-edit])])]]))))
