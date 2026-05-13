(ns feuerwehr-strichliste.components.drawer
  (:require [reagent.core :as r]))

(defn drawer [{:keys [open? on-close title]} child]
  (let [touch-start (r/atom nil)]
    (fn [{:keys [open? on-close title]} child]
      [:div.drawer-overlay
       {:class    (when open? "open")
        :on-click on-close}
       [:div.drawer
        {:class          (when open? "open")
         :on-click       #(.stopPropagation %)
         :on-touch-start #(reset! touch-start (.. % -touches (item 0) -clientX))
         :on-touch-end   (fn [e]
                           (when-let [start @touch-start]
                             (let [dx (- (.. e -changedTouches (item 0) -clientX) start)]
                               (when (> dx 80) (on-close))))
                           (reset! touch-start nil))}
        [:div.drawer-header
         [:h2.drawer-title title]
         [:button.drawer-close {:on-click on-close}
          [:span.icon [:i.fas.fa-xmark]]]]
        [:div.drawer-body child]]])))
