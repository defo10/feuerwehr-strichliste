(ns feuerwehr-strichliste.views
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.routes :as routes]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.pages.home :refer [home-page]]
   [feuerwehr-strichliste.components.error-overlay :refer [error-overlay]]))

(defmethod routes/panels :home-panel [] [home-page])

;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:on-click #(re-frame/dispatch [::events/navigate :home])}
     "go to Home Page"]]])

(defmethod routes/panels :about-panel [] [about-panel])

;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:<>
     (routes/panels @active-panel)
     [error-overlay]]))