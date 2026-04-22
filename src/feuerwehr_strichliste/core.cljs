(ns feuerwehr-strichliste.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.routes :as routes]
   [feuerwehr-strichliste.views :as views]
   [feuerwehr-strichliste.config :as config]
   [feuerwehr-strichliste.styles :as styles]
   [feuerwehr-strichliste.storage :as storage]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (styles/inject!)
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (dev-setup)
  (storage/init!
   (fn []
     (re-frame/dispatch-sync [::events/initialize-db])
     (mount-root))))
