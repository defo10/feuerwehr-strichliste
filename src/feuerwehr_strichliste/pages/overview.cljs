(ns feuerwehr-strichliste.pages.overview
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.events :as events]))

(def ^:private kitchen-actions
  [{:icon "➕" :color "#4CAF50" :title "Neues Essen/Trinken hinzufügen"}
   {:icon "✏️"  :color "#2196F3" :title "Essen/Trinken bearbeiten"}
   {:icon "📦" :color "#FF9800" :title "Bestand bearbeiten"}])

(def ^:private admin-actions
  [{:icon "👥" :color "#9C27B0" :title "Nutzer verwalten"}
   {:icon "💰" :color "#009688" :title "Einzahlungen überblicken"}])

(defn- actions-for-role [role]
  (case role
    :kitchen kitchen-actions
    :admin   (concat kitchen-actions admin-actions)
    nil))

(defn- action-button [{:keys [icon color title]}]
  [:button.action-button {:style {:background color}}
   [:span.action-icon icon]
   [:span.action-title title]])

(defn- action-bar [user]
  (when-let [actions (actions-for-role (:user/role user))]
    [:div.action-bar
     (for [{:keys [title] :as action} actions]
       ^{:key title} [action-button action])]))

(defn overview-page []
  (let [current-user (re-frame/subscribe [::subs/current-user])]
    (fn []
      (let [user @current-user]
        [:div
         [:nav.top-nav
          [:span.top-nav-name (:user/name user)]
          [:button.top-nav-logout
           {:on-click #(re-frame/dispatch [::events/sign-out])}
           "Fertig"]]
         [action-bar user]]))))
