(ns feuerwehr-strichliste.styles
  (:require [garden.core :refer [css]]
            [garden.units :refer [rem px em]]))


(comment
  (println (garden.core/css rules)))

(def ^:private tokens
  {:color-bg      "#f0f2f5"
   :color-surface "#ffffff"
   :color-text    "#1a1a2e"
   :color-border  "#e5e7eb"
   :color-hover   "#f9fafb"
   :color-active  "#e5e7eb"
   :color-accent  "#cc2936"
   :radius        (px 12)
   :shadow        "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)"
   :shadow-hover  "0 4px 12px rgba(0,0,0,0.12)"})

(defn- t [k] (get tokens k))

(def ^:private rules
  [[:* {:box-sizing "border-box" :margin 0 :padding 0}]

   [:body
    {:font-family "-apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif"
     :background  (t :color-bg)
     :color       (t :color-text)
     :min-height  "100vh"}]

   [:.page
    {:padding (rem 2)}]

   [:.page-title
    {:font-size     (rem 2)
     :font-weight   700
     :margin-bottom (rem 1.5)}]

   [:.user-list
    {:background    (t :color-surface)
     :border        "1px solid #e5e7eb"
     :border-radius (t :radius)
     :box-shadow    (t :shadow)
     :overflow      "hidden"}]

   [:.user-list-item
    {:padding     "1rem 1.5rem"
     :font-size   (rem 1.15)
     :font-weight 500
     :cursor      "pointer"
     :transition  "background 0.1s"
     :user-select "none"}
    [:& + :& {:border-top "1px solid #e5e7eb"}]
    [:&:hover  {:background (t :color-hover)}]
    [:&:active {:background (t :color-active)}]
    [:&.inactive {:opacity 0.4}]]])

(defn inject! []
  (let [el (or (.getElementById js/document "app-styles")
               (let [new-el (.createElement js/document "style")]
                 (set! (.-id new-el) "app-styles")
                 (.appendChild (.-head js/document) new-el)
                 new-el))]
    (set! (.-textContent el) (css rules))))
