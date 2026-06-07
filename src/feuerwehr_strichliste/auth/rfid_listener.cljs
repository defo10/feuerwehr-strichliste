(ns feuerwehr-strichliste.auth.rfid-listener
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.auth.events :as auth-events]))

(def ^:private buffer (atom ""))
(def ^:private timeout-id (atom nil))

(defn- reset-buffer! []
  (reset! buffer "")
  (when @timeout-id
    (js/clearTimeout @timeout-id)
    (reset! timeout-id nil)))

(defn- on-keydown [e]
  (let [key (.-key e)]
    (when @timeout-id
      (js/clearTimeout @timeout-id))
    (if (= key "Enter")
      (let [s @buffer]
        (reset-buffer!)
        (when (>= (count s) 6)
          (re-frame/dispatch [::auth-events/rfid-input s])))
      (when (= 1 (count key))
        (swap! buffer str key)
        (reset! timeout-id
                (js/setTimeout reset-buffer! 100))))))

(defn mount! []
  (.addEventListener js/document "keydown" on-keydown))
