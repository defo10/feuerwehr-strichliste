(ns feuerwehr-strichliste.top-up.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [feuerwehr-strichliste.top-up.events :as events]))

(defn- format-cents [cents]
  (str (quot cents 100) "," (let [r (mod cents 100)] (if (< r 10) (str "0" r) r))))

(defn admin-top-up-form [{:keys [user on-close]}]
  (let [amount (r/atom 0)]
    (fn [{:keys [user on-close]}]
      (let [a @amount]
        [:form.drawer-form {:on-submit #(.preventDefault %)}
         [:div.form-field
          [:label (str "Betrag für " (:user/name user))]
          [:div.price-wrapper
           [:input.price-input
            {:type            "text"
             :input-mode      "numeric"
             :value           (format-cents a)
             :on-change       identity
             :on-before-input (fn [e]
                                (.preventDefault e)
                                (when-let [d (.-data e)]
                                  (when (re-matches #"\d" d)
                                    (swap! amount (fn [c]
                                                    (let [n (+ (* c 10) (js/parseInt d))]
                                                      (if (< n 1000000) n c)))))))
             :on-key-down     (fn [e]
                                (when (= "Backspace" (.-key e))
                                  (.preventDefault e)
                                  (swap! amount #(quot % 10))))}]
           [:span.price-currency "€"]]]
         [:div.form-actions
          [:button.button.is-primary.is-fullwidth
           {:type     "submit"
            :disabled (zero? a)
            :on-click (fn [e]
                        (.preventDefault e)
                        (re-frame/dispatch [::events/admin-top-up
                                            {:user-id (:user/id user)
                                             :amount  a}])
                        (on-close))}
           "Einzahlen"]
          [:button.button.is-light.is-fullwidth
           {:type     "button"
            :on-click on-close}
           "Abbrechen"]]]))))

(defn top-up-form [{:keys [current-user on-close on-staged initial-amount]}]
  (let [form (r/atom {:user-id (:user/id current-user) :amount (or initial-amount 0)})]
    (fn [{:keys [on-close on-staged]}]
      (let [f @form]
        [:form.drawer-form {:on-submit #(.preventDefault %)}

         [:div.form-field
          [:label "Betrag"]
          [:div.price-wrapper
           [:input.price-input
            {:type            "text"
             :input-mode      "numeric"
             :value           (format-cents (:amount f))
             :on-change       identity
             :on-before-input (fn [e]
                                (.preventDefault e)
                                (when-let [d (.-data e)]
                                  (when (re-matches #"\d" d)
                                    (swap! form update :amount
                                           (fn [c]
                                             (let [n (+ (* c 10) (js/parseInt d))]
                                               (if (< n 1000000) n c)))))))
             :on-key-down     (fn [e]
                                (when (= "Backspace" (.-key e))
                                  (.preventDefault e)
                                  (swap! form update :amount #(quot % 10))))}]
           [:span.price-currency "€"]]]

         [:div.form-actions
          [:button.button.is-primary.is-fullwidth
           {:type     "submit"
            :disabled (zero? (:amount f))
            :on-click (fn [e]
                        (.preventDefault e)
                        (re-frame/dispatch [::events/stage-top-up
                                            {:user-id (:user-id f)
                                             :amount  (:amount f)}])
                        (on-close)
                        (when on-staged (on-staged)))}
           "Einzahlung vormerken"]
          [:button.button.is-light.is-fullwidth
           {:type     "button"
            :on-click on-close}
           "Abbrechen"]]]))))
