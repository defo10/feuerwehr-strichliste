(ns feuerwehr-strichliste.styles
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-keyframes]]
            [garden.units :refer [rem px]]))

(comment
  (println (garden.core/css rules)))

(def ^:private rules
  [(at-keyframes "shake"
     [:from  {:transform "rotate(0deg)"}]
     ["20%"  {:transform "rotate(-6deg)"}]
     ["40%"  {:transform "rotate(6deg)"}]
     ["60%"  {:transform "rotate(-4deg)"}]
     ["80%"  {:transform "rotate(4deg)"}]
     [:to    {:transform "rotate(0deg)"}])

   (at-keyframes "overlay-in"
     [:from {:backdrop-filter "blur(0px)" :background "rgba(0,0,0,0)"}]
     [:to   {:backdrop-filter "blur(6px)" :background "rgba(0,0,0,0.4)"}])

   (at-keyframes "modal-in"
     [:from {:opacity 0 :transform "scale(0.95) translateY(8px)"}]
     [:to   {:opacity 1 :transform "scale(1) translateY(0)"}])

   [":root"
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

   [:.error-overlay
    {:position        "fixed"
     :inset           0
     :background      "rgba(0,0,0,0.5)"
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :z-index         100}]

   [:.error-dialog
    {:background    "var(--color-surface)"
     :border-radius "var(--radius)"
     :padding       "2rem"
     :max-width      "400px"
     :width          "90%"
     :box-shadow    "var(--shadow-hover)"}]

   [:.error-title
    {:font-size     "1.25rem"
     :font-weight   700
     :margin-bottom "0.5rem"
     :color         "var(--color-primary)"}]

   [:.error-message
    {:color         "var(--color-on-surface)"
     :margin-bottom "1.5rem"}]

   [:.error-actions
    {:display         "flex"
     :gap             "0.75rem"
     :justify-content "flex-end"}]

   [:.error-reload
    {:background    "var(--color-primary)"
     :color         "var(--color-on-primary)"
     :border        "none"
     :border-radius "var(--radius)"
     :padding       "0.6rem 1.25rem"
     :font-size     "1rem"
     :font-weight   600
     :cursor        "pointer"}]

   [:.error-dismiss
    {:background    "transparent"
     :color         "var(--color-on-surface)"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :padding       "0.6rem 1.25rem"
     :font-size     "1rem"
     :cursor        "pointer"}]

   [:.pin-modal-overlay
    {:position        "fixed"
     :inset           0
     :background      "rgba(0,0,0,0.4)"
     :backdrop-filter "blur(6px)"
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :z-index         100
     :animation       "overlay-in 0.2s ease forwards"}]

   [:.pin-modal
    {:background     "var(--color-surface)"
     :border-radius  "var(--radius)"
     :padding        0
     :width          (px 320)
     :display        "flex"
     :flex-direction "column"
     :align-items    "center"
     :gap            (rem 1.5)
     :box-shadow     "var(--shadow-hover)"
     :animation      "modal-in 0.2s ease forwards"}]

   [:.pin-modal-title
    {:font-size   (rem 1.5)
     :font-weight 700
     :padding     "0 2rem"}]

   [:.pin-modal-close
    {:align-self  "flex-end"
     :background  "none"
     :border      "none"
     :cursor      "pointer"
     :color       "var(--color-on-surface-muted)"
     :font-size   (rem 2)
     :line-height 1
     :padding     "0.75rem"
     :transition  "color 0.1s"}
    [:&:hover {:color "var(--color-on-surface)"}]]

   [:.pin-dots
    {:display "flex"
     :gap     (rem 1)
     :padding "0 2rem"}
    [:&.shaking {:animation "shake 0.4s ease"}]]

   [:.pin-dot
    {:width         (px 22)
     :height        (px 22)
     :border-radius "50%"
     :border        "2px solid var(--color-outline)"
     :background    "transparent"
     :transition    "background 0.1s"}
    [:&.filled {:background   "var(--color-on-surface)"
                :border-color "var(--color-on-surface)"}]]

   [:.pin-keypad
    {:display               "grid"
     :grid-template-columns "repeat(3, 1fr)"
     :gap                   (rem 0.75)
     :width                 "100%"
     :padding               "0 2rem 2rem"}]

   [:.pin-key
    {:font-size     (rem 1.75)
     :font-weight   600
     :padding       "1rem"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :background    "var(--color-surface)"
     :cursor        "pointer"
     :transition    "background 0.1s"}
    [:&:hover  {:background "var(--color-surface-hover)"}]
    [:&:active {:background "var(--color-surface-active)"}]]

   [:.pin-error
    {:color       "var(--color-primary)"
     :font-weight 600
     :padding     "0 2rem"}]

   [:.pin-success
    {:color       "green"
     :font-weight 600
     :padding     "0 2rem"}]

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
