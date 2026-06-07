(ns feuerwehr-strichliste.pages.activity
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.events :as events]
   [feuerwehr-strichliste.subs :as app-subs]
   [feuerwehr-strichliste.user.subs :as user-subs]
   [feuerwehr-strichliste.item.subs :as item-subs]
   [feuerwehr-strichliste.item.views :refer [format-price]]))

(defn- format-date [iso]
  (.toLocaleString (js/Date. iso) "de-DE"
                   #js {:day "2-digit" :month "2-digit" :year "2-digit"
                        :hour "2-digit" :minute "2-digit"}))

(defn- local-date-str
  "Returns a YYYY-MM-DD string in local time for the given Date object."
  [^js d]
  (str (.getFullYear d) "-"
       (-> (.getMonth d) inc (.toString) (.padStart 2 "0")) "-"
       (-> (.getDate d) (.toString) (.padStart 2 "0"))))

(defn- default-date-from []
  (let [d (js/Date.)]
    (.setFullYear d (- (.getFullYear d) 1))
    (local-date-str d)))

(defn- default-date-to []
  (local-date-str (js/Date.)))

(def ^:private event-type-options
  [[:cart/checked-out         "Einkauf"]
   [:balance/top-up-requested "Einzahlung beantragt"]
   [:balance/top-up-confirmed "Einzahlung bestätigt"]
   [:balance/top-up-cancelled "Einzahlung storniert"]
   [:item/restocked           "Artikel aufgefüllt"]
   [:item/stock-corrected     "Inventur"]
   [:item/created             "Artikel erstellt"]
   [:item/edited              "Artikel bearbeitet"]
   [:user/created             "Nutzer erstellt"]
   [:user/updated             "Nutzer aktualisiert"]
   [:auth/sign-in-attempted   "Anmeldeversuch"]
   [:auth/signed-out          "Abgemeldet"]])

(def ^:private event-type-labels (into {} event-type-options))

(defn- event-type-label [event-type]
  (get event-type-labels event-type (name event-type)))

(defn- sort-value [col event users-map]
  (case col
    :time (.getTime (js/Date. (:event/timestamp event)))
    :type (name (:event/type event))
    :user (get-in users-map [(:event/actor event) :user/name] "")))

(defn- apply-sort [sort-state users-map events]
  (let [{:keys [col dir]} sort-state
        sorted (sort-by #(sort-value col % users-map) events)]
    (if (= dir :desc) (reverse sorted) sorted)))

(defn- toggle-sort [sort-state col]
  (swap! sort-state (fn [{c :col d :dir}]
                      {:col col :dir (if (and (= c col) (= d :asc)) :desc :asc)})))

(defn- col-header [sort-state col label]
  (let [{c :col d :dir} @sort-state
        active? (= c col)]
    [:button.col-header {:on-click #(toggle-sort sort-state col)}
     label (when active? (if (= d :asc) " ▲" " ▼"))]))

(defn- details-cell [event users-map items-map log]
  (case (:event/type event)
    :cart/checked-out
    (let [entries (:checkout/entries event)
          total   (reduce (fn [s {:keys [quantity unit-price]}] (+ s (* quantity unit-price))) 0 entries)]
      [:td.is-size-7.has-text-grey
       (for [{:keys [item-id quantity]} entries]
         ^{:key item-id}
         [:span (str quantity "× " (get-in items-map [item-id :item/name] "?")) " "])
       [:span.has-text-weight-semibold (str "−" (format-price total))]])

    :balance/top-up-requested
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (format-price (:top-up/amount event))]
     (str " für " (get-in users-map [(:event/subject event) :user/name] "?"))]

    :balance/top-up-cancelled
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (format-price (:top-up/amount event))]
     (str " für " (get-in users-map [(:event/subject event) :user/name] "?"))]

    :balance/top-up-confirmed
    (let [req (first (filter #(= (:event/id %) (:top-up/request-id event)) log))]
      [:td.is-size-7.has-text-grey
       (when req
         [:<>
          [:span.has-text-weight-semibold (format-price (:top-up/amount req))]
          (str " für " (get-in users-map [(:event/subject req) :user/name] "?"))])])

    :item/restocked
    [:td.is-size-7.has-text-grey
     [:span.has-text-weight-semibold (str "+" (:item/stock event))]
     (str " · " (get-in items-map [(:item/id event) :item/name] "?"))]

    :item/stock-corrected
    [:td.is-size-7.has-text-grey
     (str (get-in items-map [(:item/id event) :item/name] "?") " ")
     [:span.has-text-weight-semibold (str (:item/stock-before event) " → " (:item/stock event))]]

    :item/created  [:td.is-size-7.has-text-grey (:item/name event)]
    :item/edited   [:td.is-size-7.has-text-grey (:item/name event)]
    :user/created  [:td.is-size-7.has-text-grey (:user/name event)]
    :user/updated  [:td.is-size-7.has-text-grey (:user/name event)]

    :auth/sign-in-attempted
    [:td
     (if (:auth/success event)
       [:span.tag.is-success.is-light "Erfolg"]
       [:span.tag.is-danger.is-light "Fehlschlag"])]

    [:td]))

(defn- event-row [event users-map items-map log]
  (let [actor-id   (:event/actor event)
        subject-id (:event/subject event)
        actor-name (get-in users-map [actor-id :user/name] "?")
        nutzer     (if (and subject-id (not= subject-id actor-id))
                     (str (get-in users-map [subject-id :user/name] "?")
                          " (durch " actor-name ")")
                     actor-name)]
    [:tr
     [:td.is-size-7.has-text-grey (format-date (:event/timestamp event))]
     [:td (event-type-label (:event/type event))]
     [:td nutzer]
     [details-cell event users-map items-map log]]))

(defn- filter-bar [type-filter user-filter date-from date-to users-map]
  (let [type-open? (r/atom false)
        close!     #(reset! type-open? false)]
    (r/create-class
     {:component-did-mount    #(.addEventListener js/document "click" close!)
      :component-will-unmount #(.removeEventListener js/document "click" close!)
      :reagent-render
      (fn [type-filter user-filter date-from date-to users-map]
        [:div {:style {:margin-bottom "1rem" :display "flex" :flex-wrap "wrap" :gap "0.5rem" :align-items "center"}}
         [:div.dropdown {:class    (when @type-open? "is-active")
                         :on-click #(.stopPropagation %)}
          [:div.dropdown-trigger
           [:button.button.is-small
            {:on-click #(swap! type-open? not)}
            [:span (if (empty? @type-filter)
                     "Alle Ereignisse"
                     (str (count @type-filter) " Ereignistypen"))]
            [:span.icon.is-small [:i.fas.fa-angle-down]]]]
          [:div.dropdown-menu {:style {:min-width "14rem"}}
           [:div.dropdown-content
            (let [tf @type-filter]
              (doall
               (for [[k label] event-type-options]
                 ^{:key k}
                 [:label.dropdown-item {:style {:display "flex" :gap "0.5rem" :cursor "pointer"}}
                  [:input {:type      "checkbox"
                           :checked   (contains? tf k)
                           :on-change #(swap! type-filter (if (contains? @type-filter k) disj conj) k)}]
                  label])))]]]
         [:div.select.is-small
          [:select {:value     (or @user-filter "")
                    :on-change #(reset! user-filter (let [v (.. % -target -value)]
                                                      (when (seq v) v)))}
           [:option {:value ""} "Alle Nutzer"]
           (for [[id u] (sort-by (comp :user/name val) users-map)]
             ^{:key id} [:option {:value id} (:user/name u)])]]
         [:input.input.is-small
          {:type      "date"
           :style     {:width "auto"}
           :value     (or @date-from "")
           :on-change #(reset! date-from (let [v (.. % -target -value)]
                                           (when (seq v) v)))}]
         [:input.input.is-small
          {:type      "date"
           :style     {:width "auto"}
           :value     (or @date-to "")
           :on-change #(reset! date-to (let [v (.. % -target -value)]
                                         (when (seq v) v)))}]
         (when (or (seq @type-filter) @user-filter
                   (not= @date-from (default-date-from))
                   (not= @date-to (default-date-to)))
           [:button.button.is-small.is-ghost
            {:on-click #(do (reset! type-filter #{})
                            (reset! user-filter nil)
                            (reset! date-from (default-date-from))
                            (reset! date-to (default-date-to)))}
            "Zurücksetzen"])])})))

(defn- log-table [events sort-state um im log]
  (if (empty? events)
    [:p.has-text-grey "Keine Aktivitäten vorhanden."]
    [:div {:style {:background    "var(--color-surface)"
                   :border        "1px solid var(--color-outline)"
                   :border-radius "var(--radius)"
                   :box-shadow    "var(--shadow)"
                   :overflow      "hidden"}}
     [:div.table-container
      [:table.table.is-fullwidth.is-hoverable
       [:thead
        [:tr
         [:th [col-header sort-state :time "Zeit"]]
         [:th [col-header sort-state :type "Ereignis"]]
         [:th [col-header sort-state :user "Nutzer"]]
         [:th "Details"]]]
       [:tbody
        (for [event events]
          ^{:key (:event/id event)}
          [event-row event um im log])]]]]))

(defn activity-page []
  (let [activity-log (re-frame/subscribe [::app-subs/activity-log])
        users-map    (re-frame/subscribe [::user-subs/users-map])
        items-map    (re-frame/subscribe [::item-subs/items-map])
        sort-state   (r/atom {:col :time :dir :desc})
        type-filter  (r/atom #{})
        user-filter  (r/atom nil)
        date-from    (r/atom (default-date-from))
        date-to      (r/atom (default-date-to))]
    (r/create-class
     {:component-did-mount
      (fn [_] (re-frame/dispatch [::events/load-activity-log]))
      :component-will-unmount
      (fn [_] (re-frame/dispatch [:activity-log/loaded nil]))
      :reagent-render
      (fn []
        (let [log      @activity-log
              um       @users-map
              im       @items-map
              tf       @type-filter
              uf       @user-filter
              df       @date-from
              dt       @date-to
              ss       @sort-state
              filtered (when log
                         (->> log
                              (filter (fn [e]
                                        (or (empty? tf)
                                            (contains? tf (:event/type e)))))
                              (filter (fn [e]
                                        (or (nil? uf)
                                            (= uf (:event/actor e))
                                            (= uf (:event/subject e)))))
                              (filter (fn [e]
                                        (let [t (.getTime (js/Date. (:event/timestamp e)))]
                                          (and (or (nil? df)
                                                   (>= t (.getTime (js/Date. df))))
                                               (or (nil? dt)
                                                   (<= t (+ (.getTime (js/Date. dt)) 86399999)))))))
                              (apply-sort ss um)
                              vec))]
          [:div
           [:nav.top-nav
            [:button.button.is-ghost
             {:on-click #(re-frame/dispatch [::events/navigate :overview])}
             [:span.icon [:i.fas.fa-arrow-left]]
             [:span "Zurück"]]
            [:span.top-nav-name "Aktivitätslog"]
            [:span]]

           [:div {:style {:padding "1.5rem"}}
            [filter-bar type-filter user-filter date-from date-to um]
            (if (nil? log)
              [:p.has-text-grey "Laden…"]
              [log-table filtered sort-state um im log])]]))})))
