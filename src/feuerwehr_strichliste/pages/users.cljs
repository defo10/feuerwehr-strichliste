(ns feuerwehr-strichliste.pages.users
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.user.events :as user-events]
   [feuerwehr-strichliste.user.views :refer [new-user-form edit-user-form role-labels status-labels]]
   [feuerwehr-strichliste.components.drawer :refer [drawer]]))

(defn- role-select [user]
  [:select.role-select
   {:value     (name (:user/role user))
    :on-change #(re-frame/dispatch [::user-events/user-update
                                    {:id     (:user/id user)
                                     :name   (:user/name user)
                                     :role   (keyword (.. % -target -value))
                                     :status (:user/status user)}])}
   (for [[k label] role-labels]
     ^{:key k} [:option {:value (name k)} label])])

(defn users-page []
  (let [all-users    (re-frame/subscribe [::user-subs/all-users])
        editing-user (re-frame/subscribe [::user-subs/editing-user])
        can-manage?  (re-frame/subscribe [::user-subs/can-manage-users?])
        add-open?    (r/atom false)]
    (fn []
      [:<>
       [:div
        [:nav.top-nav
         [:button.top-nav-back
          {:on-click #(re-frame/dispatch [::events/navigate :overview])}
          "←"]
         [:span.top-nav-name "Nutzer"]
         (when @can-manage?
           [:button.top-nav-logout
            {:on-click #(reset! add-open? true)}
            "+ Hinzufügen"])]

        [:div.users-table
         [:div.users-table-header
          [:span "Name"]
          [:span "Rolle"]
          [:span "Status"]
          [:span]]
         (for [user @all-users]
           ^{:key (:user/id user)}
           [:div.users-table-row
            [:span.users-table-name (:user/name user)]
            (if @can-manage?
              [role-select user]
              [:span.role-label (role-labels (:user/role user))])
            [:span.status-badge {:class (name (:user/status user))}
             (status-labels (:user/status user))]
            (if @can-manage?
              [:button.item-card-edit
               {:on-click #(re-frame/dispatch [::user-events/edit-user user])}
               "✏️"]
              [:span])])]]

       [drawer {:open?    @add-open?
                :on-close #(reset! add-open? false)
                :title    "Neuer Nutzer"}
        [new-user-form #(reset! add-open? false)]]

       [drawer {:open?    (some? @editing-user)
                :on-close #(re-frame/dispatch [::user-events/close-edit])
                :title    "Nutzer bearbeiten"}
        (when @editing-user
          [edit-user-form @editing-user :admin
           #(re-frame/dispatch [::user-events/close-edit])])]])))
