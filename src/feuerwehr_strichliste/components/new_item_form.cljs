(ns feuerwehr-strichliste.components.new-item-form
  (:require [reagent.core :as r]
            [clojure.string :as str]))

(defn- format-cents [cents]
  (str (quot cents 100) "," (let [r (mod cents 100)] (if (< r 10) (str "0" r) r))))

(defn- valid? [{:keys [name price-cents stock]}]
  (and (not (str/blank? name))
       (pos? price-cents)
       (not (str/blank? stock))
       (let [n (js/parseInt stock)]
         (and (not (js/isNaN n)) (>= n 0)))))

(defn new-item-form [_on-close]
  (let [form (r/atom {:type "food" :name "" :price-cents 0 :stock "0"})]
    (fn [on-close]
      (let [f @form]
        [:form.drawer-form {:on-submit #(.preventDefault %)}

         [:div.form-field
          [:label "Typ"]
          [:div.type-picker
           (for [[value label] [["food" "Essen"] ["drink" "Trinken"]]]
             ^{:key value}
             [:label.type-option {:class (when (= (:type f) value) "selected")}
              [:input {:type      "radio"
                       :name      "type"
                       :value     value
                       :checked   (= (:type f) value)
                       :on-change #(swap! form assoc :type value)}]
              label])]]

         [:div.form-field
          [:label "Name"]
          [:input {:type        "text"
                   :placeholder "z.B. Apfelsaft"
                   :value       (:name f)
                   :on-change   #(swap! form assoc :name (.. % -target -value))}]]

         [:div.form-field
          [:label
           "Bild "
           [:span.optional-hint "(optional)"]]
          [:input.optional-input {:type "file" :accept "image/*"}]]

         [:div.form-field
          [:label "Preis pro Einheit"]
          [:div.price-wrapper
           [:input.price-input
            {:type            "text"
             :input-mode      "numeric"
             :value           (format-cents (:price-cents f))
             :on-change       identity
             :on-before-input (fn [e]
                                (.preventDefault e)
                                (when-let [d (.-data e)]
                                  (when (re-matches #"\d" d)
                                    (swap! form update :price-cents
                                           (fn [c]
                                             (let [n (+ (* c 10) (js/parseInt d))]
                                               (if (< n 1000000) n c)))))))
             :on-key-down     (fn [e]
                                (when (= "Backspace" (.-key e))
                                  (.preventDefault e)
                                  (swap! form update :price-cents #(quot % 10))))}]
           [:span.price-currency "€"]]]

         [:div.form-field
          [:label "Vorrat"]
          [:input {:type      "number"
                   :min       "0"
                   :step      "1"
                   :value     (:stock f)
                   :on-change #(swap! form assoc :stock (.. % -target -value))}]]

         [:div.form-actions
          [:button.form-submit
           {:type     "submit"
            :disabled (not (valid? f))}
           "Hinzufügen"]]]))))
