(ns feuerwehr-strichliste.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.subs :as subs]
   [feuerwehr-strichliste.config :as config]
   [feuerwehr-strichliste.styles :as styles]
   [feuerwehr-strichliste.domain.storage :as storage]
   [feuerwehr-strichliste.domain.effects]
   [feuerwehr-strichliste.components.error-overlay :refer [error-overlay]]
   [feuerwehr-strichliste.pages.home :refer [home-page]]
   [feuerwehr-strichliste.pages.overview :refer [overview-page]]))

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])
(defmethod panels :home-panel [] [home-page])
(defmethod panels :overview-panel [] [overview-page])

(def routes ["/" {""         :home
                  "overview" :overview}])

(defn- parse [url]
  (bidi/match-route routes url))

(defn- dispatch-route [route]
  (re-frame/dispatch [::events/set-active-panel
                      (keyword (str (name (:handler route)) "-panel"))]))

(defonce history
  (pushy/pushy dispatch-route parse))

(re-frame/reg-fx
 :navigate
 (fn [handler]
   (pushy/set-token! history (bidi/path-for routes handler))))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:<>
     (panels @active-panel)
     [error-overlay]]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (styles/inject!)
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-panel] root-el)))

(defn init []
  (pushy/start! history)
  (dev-setup)
  (storage/init!
   (fn []
     (re-frame/dispatch-sync [::events/initialize-db])
     (mount-root))))
