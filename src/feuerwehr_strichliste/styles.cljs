(ns feuerwehr-strichliste.styles
  (:require [garden.core :refer [css]]
            [garden.units :refer [rem px]]))

(comment
  (println (garden.core/css rules)))

(def ^:private rules
  [[":root"
    {"--color-primary"          "#cc2936"
     "--color-on-primary"       "#ffffff"
     "--color-background"       "#f0f2f5"
     "--color-surface"          "#ffffff"
     "--color-on-surface"       "#1a1a2e"
     "--color-outline"          "#e5e7eb"
     "--color-surface-hover"    "#f9fafb"
     "--color-surface-active"   "#e5e7eb"
     "--color-on-surface-muted" "#c0c4cc"
     "--shadow"                 "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)"
     "--shadow-hover"           "0 4px 12px rgba(0,0,0,0.12)"
     "--radius"                 "12px"}]

   [:* {:box-sizing "border-box" :margin 0 :padding 0}]

   [:body
    {:font-family "-apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif"
     :background  "var(--color-background)"
     :color       "var(--color-on-surface)"
     :min-height  "100vh"}]

   [:.page
    {:padding "2rem 3.75rem 2rem 2rem"}]

   [:.page-header
    {:display        "flex"
     :align-items    "center"
     :justify-content "space-between"
     :margin-bottom  (rem 1.5)
     :gap            (rem 1)}]

   [:.page-title
    {:font-size   (rem 2)
     :font-weight 700}]

   [:.search-bar
    {:font-size     (rem 1)
     :padding       "0.5rem 0.75rem"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :background    "var(--color-surface)"
     :color         "var(--color-on-surface)"
     :outline       "none"
     :width         (px 200)
     :transition    "border-color 0.15s, width 0.2s ease"}
    [:&:focus {:border-color "var(--color-primary)" :width (px 260)}]]

   [:.user-list
    {:background    "var(--color-surface)"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :box-shadow    "var(--shadow)"
     :overflow      "hidden"}]

   [:.user-list-item
    {:padding     "1rem 1.5rem"
     :font-size   (rem 1.15)
     :font-weight 500
     :cursor      "pointer"
     :transition  "background 0.1s"
     :user-select "none"}
    [:& + :& {:border-top "1px solid var(--color-outline)"}]
    [:.user-list > :div + :div > :& {:border-top "1px solid var(--color-outline)"}]
    [:&:hover  {:background "var(--color-surface-hover)"}]
    [:&:active {:background "var(--color-surface-active)"}]]

   [:.alphabet-bar
    {:position       "fixed"
     :right          0
     :top            0
     :width          (px 44)
     :height         "100vh"
     :display        "flex"
     :flex-direction "column"
     :padding        "0.5rem 0"}]

   [:.alphabet-bar-letter
    {:flex            "1"
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :font-size       (rem 0.95)
     :font-weight     600
     :border-radius   (px 4)
     :cursor          "pointer"
     :color           "var(--color-on-surface)"
     :user-select     "none"
     :transition      "none"}
    [:&:hover {:background "var(--color-outline)"}]
    [:&.inactive {:color "var(--color-on-surface-muted)" :cursor "default"}]
    [:&.active {:background "var(--color-primary)" :color "var(--color-on-primary)"
                :transition "background 0.25s ease, color 0.25s ease"}]]])

(defn inject! []
  (let [el (or (.getElementById js/document "app-styles")
               (let [new-el (.createElement js/document "style")]
                 (set! (.-id new-el) "app-styles")
                 (.appendChild (.-head js/document) new-el)
                 new-el))]
    (set! (.-textContent el) (css rules))))
