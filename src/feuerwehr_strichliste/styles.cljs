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

   [:.drawer-overlay
    {:position       "fixed"
     :inset          0
     :background     "rgba(0,0,0,0)"
     :z-index        200
     :pointer-events "none"
     :transition     "background 0.3s ease, backdrop-filter 0.3s ease"}
    [:&.open {:background      "rgba(0,0,0,0.4)"
              :backdrop-filter "blur(4px)"
              :pointer-events  "all"}]]

   [:.drawer
    {:position       "fixed"
     :top            0
     :right          0
     :height         "100vh"
     :width          (px 420)
     :max-width      "90vw"
     :background     "var(--color-surface)"
     :box-shadow     "-4px 0 24px rgba(0,0,0,0.12)"
     :display        "flex"
     :flex-direction "column"
     :transform      "translateX(100%)"
     :transition     "transform 0.3s ease"
     :overflow       "hidden"}
    [:&.open {:transform "translateX(0)"}]]

   [:.drawer-header
    {:display         "flex"
     :align-items     "center"
     :justify-content "space-between"
     :padding         "1.25rem 1.5rem"
     :border-bottom   "1px solid var(--color-outline)"
     :flex-shrink     0}]

   [:.drawer-title
    {:font-size   (rem 1.25)
     :font-weight 700}]

   [:.drawer-close
    {:background  "none"
     :border      "none"
     :font-size   (rem 1.75)
     :line-height 1
     :cursor      "pointer"
     :color       "var(--color-on-surface-muted)"
     :padding     "0.25rem"
     :transition  "color 0.1s"}
    [:&:hover {:color "var(--color-on-surface)"}]]

   [:.drawer-body
    {:flex       1
     :overflow-y "auto"}]

   [:.drawer-form
    {:display        "flex"
     :flex-direction "column"
     :gap            (rem 1.5)
     :padding        "1.5rem"}]

   [:.form-field
    {:display        "flex"
     :flex-direction "column"
     :gap            "0.375rem"}
    [:label {:font-size   (rem 0.875)
             :font-weight 600}]
    [:input :select
     {:padding       "0.625rem 0.75rem"
      :border        "1px solid var(--color-outline)"
      :border-radius "var(--radius)"
      :font-size     (rem 1)
      :color         "var(--color-on-surface)"
      :background    "var(--color-surface)"
      :outline       "none"
      :width         "100%"}
     [:&:focus {:border-color "var(--color-primary)"}]]]

   [:.optional-hint
    {:color       "var(--color-on-surface-muted)"
     :font-weight 400
     :font-size   (rem 0.8)}]

   [:.optional-input
    {:border-style "dashed !important"
     :color        "var(--color-on-surface-muted)"}]

   [:.image-preview
    {:display       "block"
     :max-width     "100%"
     :max-height    (px 160)
     :border-radius "var(--radius)"
     :object-fit    "contain"
     :margin-top    "0.25rem"}]

   [:.type-picker
    {:display       "flex"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :overflow      "hidden"}]

   [:.type-option
    {:flex            1
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :padding         "0.625rem"
     :font-size       (rem 1)
     :font-weight     500
     :cursor          "pointer"
     :transition      "background 0.15s, color 0.15s"
     :user-select     "none"}
    [:& + :& {:border-left "1px solid var(--color-outline)"}]
    [:&.selected {:background "var(--color-primary)"
                  :color      "var(--color-on-primary)"
                  :font-weight 600}]
    [:input {:display "none"}]]

   [:.price-wrapper
    {:display       "flex"
     :align-items   "stretch"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :overflow      "hidden"}]

   [:.price-currency
    {:padding     "0.625rem 0.75rem"
     :color       "var(--color-on-surface-muted)"
     :font-weight 600
     :display     "flex"
     :align-items "center"}]

   [:.price-input
    {:flex                 1
     :padding              "0.625rem 0.75rem"
     :border               "none"
     :outline              "none"
     :text-align           "right"
     :font-size            (rem 1)
     :font-weight          600
     :font-variant-numeric "tabular-nums"
     :caret-color          "transparent"
     :background           "transparent"
     :width                "100%"}]

   [:.form-actions
    {:padding-top (rem 0.5)}]

   [:.form-submit
    {:width         "100%"
     :padding       "0.75rem"
     :background    "var(--color-primary)"
     :color         "var(--color-on-primary)"
     :border        "none"
     :border-radius "var(--radius)"
     :font-size     (rem 1)
     :font-weight   600
     :cursor        "pointer"
     :transition    "opacity 0.1s"}
    ["&:hover:not(:disabled)" {:opacity 0.9}]
    [:&:disabled {:opacity 0.4 :cursor "not-allowed"}]]

   [:.top-nav
    {:position        "sticky"
     :top             0
     :display         "flex"
     :align-items     "center"
     :justify-content "space-between"
     :gap             (rem 1)
     :padding         "1rem 1.5rem"
     :background      "var(--color-surface)"
     :border-bottom   "1px solid var(--color-outline)"
     :z-index         10}]

   [:.top-nav-name
    {:font-weight 600
     :font-size   (rem 1.1)}]

   [:.action-bar
    {:display    "flex"
     :gap        (rem 0.75)
     :padding    "1rem 1.5rem"
     :overflow-x "auto"}]

   [:.action-button
    {:display        "flex"
     :flex-direction "column"
     :align-items    "flex-start"
     :justify-content "space-between"
     :width          (px 140)
     :height         (px 140)
     :border         "none"
     :border-radius  "var(--radius)"
     :cursor         "pointer"
     :padding        "1rem"
     :flex-shrink    0
     :transition     "opacity 0.1s"}
    [:&:hover {:opacity 0.85}]]

   [:.action-icon
    {:font-size (rem 2)}]

   [:.action-title
    {:font-size   (rem 0.9)
     :font-weight 600
     :text-align  "left"
     :color       "#ffffff"
     :line-height 1.3}]

   [:.top-nav-logout
    {:background  "none"
     :border      "none"
     :font-size   (rem 1)
     :font-weight 600
     :color       "var(--color-primary)"
     :cursor      "pointer"
     :padding     0}
    [:&:hover {:opacity 0.8}]]

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

   [:.tab-bar
    {:display       "flex"
     :gap           (rem 0.25)
     :padding       "0.75rem 1.5rem 0"
     :background    "var(--color-surface)"
     :border-bottom "1px solid var(--color-outline)"}]

   [:.tab
    {:padding       "0.625rem 1.5rem"
     :font-size     (rem 1)
     :font-weight   600
     :border        "none"
     :border-bottom "3px solid transparent"
     :background    "transparent"
     :color         "var(--color-on-surface-muted)"
     :cursor        "pointer"
     :transition    "color 0.15s, border-color 0.15s"}
    ["&.tab--active" {:color        "var(--color-primary)"
                      :border-color "var(--color-primary)"}]
    [:&:hover {:color "var(--color-on-surface)"}]]

   [:.item-grid
    {:display               "grid"
     :grid-template-columns "repeat(auto-fill, minmax(180px, 1fr))"
     :gap                   (rem 1)
     :padding               "1.5rem"}]

   [:.item-card
    {:background     "var(--color-surface)"
     :border         "2px solid var(--color-outline)"
     :border-radius  "var(--radius)"
     :box-shadow     "var(--shadow)"
     :padding        "1rem 1.25rem"
     :display        "flex"
     :flex-direction "column"
     :gap            "0.5rem"
     :transition     "border-color 0.15s, background 0.15s"}
    ["&.item-card--selected" {:border-color "var(--color-primary)"
                              :background   "#fff5f5"}]
    ["&.item-card--empty"    {:opacity "0.45"}]]

   [:.item-card-image
    {:width         "100%"
     :height        (px 120)
     :object-fit    "cover"
     :border-radius (px 8)
     :display       "block"}]

   [:.item-card-header
    {:display        "flex"
     :flex-direction "column"
     :gap            "0.25rem"}]

   [:.item-card-name
    {:font-size   (rem 1.1)
     :font-weight 700
     :line-height "1.3"
     :flex        1}]

   [:.item-card-meta
    {:font-size   (rem 0.875)
     :color       "var(--color-on-surface-muted)"
     :white-space "nowrap"}]

   [:.item-card-controls
    {:display     "flex"
     :align-items "center"
     :justify-content "space-evenly"
     :gap         (rem 0.75)
     :margin      "0.125rem 0"}]

   [:.item-card-btn
    {:width           (px 44)
     :height          (px 44)
     :border-radius   "50%"
     :border          "2px solid var(--color-outline)"
     :background      "var(--color-surface)"
     :font-size       (rem 1.375)
     :font-weight     300
     :cursor          "pointer"
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :line-height     1
     :flex-shrink     0
     :transition      "border-color 0.1s, background 0.1s"}
    ["&:hover:not(:disabled)" {:border-color "var(--color-primary)"
                               :background   "var(--color-surface-hover)"}]
    [:&:disabled {:opacity "0.25" :cursor "not-allowed"}]]

   [:.item-card-qty
    {:font-size             (rem 1.25)
     :font-weight           700
     :min-width             (px 28)
     :text-align            "center"
     :font-variant-numeric  "tabular-nums"}]


   [:.receipt-overlay
    {:position        "fixed"
     :inset           0
     :background      "rgba(0,0,0,0.55)"
     :backdrop-filter "blur(4px)"
     :display         "flex"
     :align-items     "center"
     :justify-content "center"
     :z-index         50}]

   [:.receipt
    {:background     "var(--color-surface)"
     :border-radius  "var(--radius)"
     :width          (px 480)
     :max-width      "90vw"
     :max-height     "85vh"
     :overflow-y     "auto"
     :box-shadow     "var(--shadow-hover)"
     :display        "flex"
     :flex-direction "column"
     :gap            (rem 1)
     :padding        "2rem"}]

   [:.receipt-title
    {:font-size   (rem 1.5)
     :font-weight 700
     :text-align  "center"}]

   [:.receipt-entries
    {:display        "flex"
     :flex-direction "column"
     :gap            "0.625rem"}]

   [:.receipt-entry
    {:display     "flex"
     :align-items "baseline"
     :gap         (rem 0.75)}]

   [:.receipt-entry-name
    {:font-size   (rem 1.1)
     :font-weight 600
     :flex        1}]

   [:.receipt-entry-qty
    {:font-size  (rem 1.1)
     :color      "var(--color-on-surface-muted)"
     :min-width  (px 36)
     :text-align "center"}]

   [:.receipt-entry-price
    {:font-size             (rem 1.1)
     :font-weight           600
     :font-variant-numeric  "tabular-nums"
     :text-align            "right"
     :min-width             (px 72)}]

   [:.receipt-divider
    {:height     (px 1)
     :background "var(--color-outline)"
     :margin     "0.25rem 0"}]

   [:.receipt-total
    {:display         "flex"
     :justify-content "space-between"
     :align-items     "baseline"}
    [:span {:font-size (rem 1.1) :font-weight 600}]]

   [:.receipt-total-amount
    {:font-size             (rem 1.5)
     :font-weight           700
     :font-variant-numeric  "tabular-nums"
     :color                 "var(--color-primary)"}]

   [:.receipt-actions
    {:display    "flex"
     :gap        "0.75rem"
     :margin-top "0.5rem"}]

   [:.receipt-confirm
    {:flex          1
     :padding       "0.875rem"
     :background    "var(--color-primary)"
     :color         "var(--color-on-primary)"
     :border        "none"
     :border-radius "var(--radius)"
     :font-size     (rem 1.1)
     :font-weight   600
     :cursor        "pointer"
     :transition    "opacity 0.1s"}
    [:&:hover {:opacity 0.9}]]

   [:.receipt-edit
    {:padding       "0.875rem 1.25rem"
     :background    "transparent"
     :color         "var(--color-on-surface)"
     :border        "1px solid var(--color-outline)"
     :border-radius "var(--radius)"
     :font-size     (rem 1.1)
     :font-weight   600
     :cursor        "pointer"
     :transition    "background 0.1s"}
    [:&:hover {:background "var(--color-surface-hover)"}]]

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
