(ns feuerwehr-strichliste.item.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [feuerwehr-strichliste.item.events :as events]
            [clojure.string :as str]))

(defn- format-cents [cents]
  (str (quot cents 100) "," (let [r (mod cents 100)] (if (< r 10) (str "0" r) r))))

(defn- valid? [{:item/keys [name price]}]
  (and (not (str/blank? name))
       (pos? price)))

(defn- compress-image! [file form]
  (let [tmp-url (js/URL.createObjectURL file)
        img     (js/Image.)]
    (set! (.-onload img)
          (fn []
            (let [max-size 800
                  w        (.-naturalWidth img)
                  h        (.-naturalHeight img)
                  scale    (min 1 (/ max-size (max w h)))
                  tw       (js/Math.round (* w scale))
                  th       (js/Math.round (* h scale))
                  canvas   (.createElement js/document "canvas")]
              (set! (.-width canvas) tw)
              (set! (.-height canvas) th)
              (.drawImage (.getContext canvas "2d") img 0 0 tw th)
              (js/URL.revokeObjectURL tmp-url)
              (.toBlob canvas
                       (fn [blob]
                         (when-let [old (:image-preview @form)]
                           (js/URL.revokeObjectURL old))
                         (swap! form assoc
                                :image         blob
                                :image-preview (js/URL.createObjectURL blob)))
                       "image/jpeg"
                       0.85))))
    (set! (.-src img) tmp-url)))

(defn new-item-form [_on-close]
  (let [form (r/atom {:item/type :food :item/name "" :item/price 0 :item/stock 0})]
    (fn [on-close]
      (let [f @form]
        [:form.drawer-form {:on-submit #(.preventDefault %)}

         [:div.form-field
          [:label "Typ"]
          [:div.type-picker
           (for [[value label] [[:food "Essen"] [:drink "Trinken"]]]
             ^{:key value}
             [:label.type-option {:class (when (= (:item/type f) value) "selected")}
              [:input {:type      "radio"
                       :name      "type"
                       :value     value
                       :checked   (= (:item/type f) value)
                       :on-change #(swap! form assoc :item/type (keyword value))}]
              label])]]

         [:div.form-field
          [:label "Name"]
          [:input {:type        "text"
                   :placeholder "z.B. Apfelsaft"
                   :value       (:item/name f)
                   :on-change   #(swap! form assoc :item/name (.. % -target -value))}]]

         [:div.form-field
          [:label
           "Bild "
           [:span.optional-hint "(optional)"]]
          [:input.optional-input {:type      "file"
                                  :accept    "image/*"
                                  :capture   "environment"
                                  :on-change #(when-let [file (-> % .-target .-files (aget 0))]
                                                (compress-image! file form))}]
          (when-let [url (:image-preview f)]
            [:img.image-preview {:src url :alt "Vorschau"}])]

         [:div.form-field
          [:label "Preis pro Einheit"]
          [:div.price-wrapper
           [:input.price-input
            {:type            "text"
             :input-mode      "numeric"
             :value           (format-cents (:item/price f))
             :on-change       identity
             :on-before-input (fn [e]
                                (.preventDefault e)
                                (when-let [d (.-data e)]
                                  (when (re-matches #"\d" d)
                                    (swap! form update :item/price
                                           (fn [c]
                                             (let [n (+ (* c 10) (js/parseInt d))]
                                               (if (< n 1000000) n c)))))))
             :on-key-down     (fn [e]
                                (when (= "Backspace" (.-key e))
                                  (.preventDefault e)
                                  (swap! form update :item/price #(quot % 10))))}]
           [:span.price-currency "€"]]]

         [:div.form-field
          [:label "Vorrat"]
          [:input.price-input
           {:type            "text"
            :input-mode      "numeric"
            :value           (str (:item/stock f))
            :on-change       identity
            :on-before-input (fn [e]
                               (.preventDefault e)
                               (when-let [d (.-data e)]
                                 (when (re-matches #"\d" d)
                                   (swap! form update :item/stock
                                          (fn [s]
                                            (let [n (+ (* s 10) (js/parseInt d))]
                                              (if (< n 100000) n s)))))))
            :on-key-down     (fn [e]
                               (when (= "Backspace" (.-key e))
                                 (.preventDefault e)
                                 (swap! form update :item/stock #(quot % 10))))}]]

         [:div.form-actions
          [:button.form-submit
           {:type     "submit"
            :disabled (not (valid? f))
            :on-click (fn [e]
                        (.preventDefault e)
                        (re-frame/dispatch [::events/item-create
                                            {:item/type  (:item/type f)
                                             :item/name  (:item/name f)
                                             :item/price (:item/price f)
                                             :item/stock (:item/stock f)
                                             :image      (:image f)}])
                        (on-close))}
           "Hinzufügen"]]]))))
