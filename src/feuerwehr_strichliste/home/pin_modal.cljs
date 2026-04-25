(ns feuerwehr-strichliste.home.pin-modal
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.home.events :as events]))

(defn- pin-dots [digits error]
  [:div.pin-dots {:class (when error "shaking")}
   (for [i (range 4)]
     ^{:key i} [:div.pin-dot {:class (when (< i (count digits)) "filled")}])])

(defn- keypad []
  [:div.pin-keypad
   (for [n (range 1 10)]
     ^{:key n} [:button.pin-key {:on-click #(re-frame/dispatch [::events/pin-digit (str n)])} n])
   [:div.pin-key-spacer]
   [:button.pin-key {:on-click #(re-frame/dispatch [::events/pin-digit "0"])} "0"]
   [:button.pin-key {:on-click #(re-frame/dispatch [::events/pin-backspace])} "←"]])

(defn pin-modal [{:keys [user digits error success]}]
  [:div.pin-modal-overlay
   {:on-click #(re-frame/dispatch [::events/close-pin-modal])}
   [:div.pin-modal
    {:on-click #(.stopPropagation %)}
    [:button.pin-modal-close {:on-click #(re-frame/dispatch [::events/close-pin-modal])} "×"]
    [:h2.pin-modal-title (:user/name user)]
    [pin-dots digits error]
    [:p.pin-error (or error " ")]
    [keypad]]])
