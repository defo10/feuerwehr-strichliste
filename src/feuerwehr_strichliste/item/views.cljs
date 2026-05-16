(ns feuerwehr-strichliste.item.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [feuerwehr-strichliste.item.events :as events]
            [feuerwehr-strichliste.item.subs :as subs]
            [clojure.string :as str]))

(defn format-price [cents]
  (str (quot cents 100) "," (let [r (mod cents 100)] (if (< r 10) (str "0" r) r)) " €"))

;;
;; Item card
;;

(defn item-card [{:item/keys [id] :as item}]
  (let [qty-sub     (re-frame/subscribe [::subs/cart-qty id])
        image-sub   (re-frame/subscribe [::subs/item-image-url id])
        can-manage? (re-frame/subscribe [::subs/can-manage-items?])]
    (fn [{:item/keys [id name price stock] :as item}]
      (let [qty        @qty-sub
            image-url  @image-sub
            available? (pos? stock)
            selected?  (pos? qty)
            at-max?    (>= qty stock)]
        [:div.item-card {:class (str (when selected? "item-card--selected ")
                                     (when-not available? "item-card--empty"))}
         (when @can-manage?
           [:div.item-card-actions
            [:button.item-card-edit
             {:on-click #(re-frame/dispatch [::events/edit-item item])}
             [:span.icon.is-small [:i.fas.fa-pencil]]]])
         (when image-url
           [:img.item-card-image {:src image-url :alt name}])
         [:div.item-card-header
          [:span.item-card-name name]
          [:span.item-card-meta (if available?
                                  (str (format-price price) " / " stock " übrig")
                                  (str (format-price price) " / Ausverkauft"))]]
         [:div.item-card-controls
          [:button.item-card-btn
           {:disabled (or (not available?) (zero? qty))
            :on-click #(re-frame/dispatch [::events/decrement id])}
           "−"]
          [:span.item-card-qty (str qty)]
          [:button.item-card-btn
           {:disabled (or (not available?) at-max?)
            :on-click #(re-frame/dispatch [::events/increment id])}
           "+"]]]))))

;;
;; Item form (shared between new and edit)
;;

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

(defn- item-form [{:keys [initial existing-image-url submit-label on-submit]}]
  (let [form (r/atom (merge {:item/type :food :item/name "" :item/price 0 :item/stock 0}
                            (select-keys initial [:item/type :item/name :item/price :item/stock])))]
    (fn [{:keys [existing-image-url submit-label on-submit]}]
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
          [:input.input {:type        "text"
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
          (when-let [url (or (:image-preview f) existing-image-url)]
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
          [:button.button.is-primary.is-fullwidth
           {:type     "submit"
            :disabled (not (valid? f))
            :on-click (fn [e]
                        (.preventDefault e)
                        (on-submit {:item/type  (:item/type f)
                                    :item/name  (:item/name f)
                                    :item/price (:item/price f)
                                    :item/stock (:item/stock f)
                                    :image      (:image f)}))}
           submit-label]]]))))

(defn new-item-form [on-close]
  [item-form {:submit-label "Hinzufügen"
              :on-submit    (fn [data]
                              (re-frame/dispatch [::events/item-create data])
                              (on-close))}])

(defn edit-item-form [item on-close]
  (let [image-sub (re-frame/subscribe [::subs/item-image-url (:item/id item)])]
    (fn [item on-close]
      [item-form {:initial            (select-keys item [:item/type :item/name :item/price :item/stock])
                  :existing-image-url @image-sub
                  :submit-label       "Speichern"
                  :on-submit          (fn [data]
                                        (re-frame/dispatch [::events/item-update
                                                            (assoc data :item/id (:item/id item))])
                                        (on-close))}])))
