(ns feuerwehr-strichliste.components.dev-toolbar
  (:require [feuerwehr-strichliste.domain.storage :as storage]
            [feuerwehr-strichliste.config :as config]))

(defn dev-toolbar []
  (when config/debug?
    [:div {:style {:position   "fixed"
                   :top        0
                   :left       0
                   :z-index    9999
                   :display    "flex"
                   :gap        "4px"
                   :padding    "4px"
                   :background "rgba(0,0,0,0.6)"}}
     [:button {:on-click #(storage/clear-store! (fn [] (.reload js/location)))}
      "Reset Store"]]))
