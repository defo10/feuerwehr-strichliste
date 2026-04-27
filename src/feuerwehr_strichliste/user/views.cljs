(ns feuerwehr-strichliste.user.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [clojure.string :as str]
            [feuerwehr-strichliste.user.events :as events]
            [feuerwehr-strichliste.auth.events :as auth-events]))

(def role-labels
  {:member "Mitglied" :kitchen "Küche" :admin "Admin"})

(def status-labels
  {:active "Aktiv" :inactive "Inaktiv" :suspended "Gesperrt"})

(defn user-card [user]
  [:div.user-list-item
   {:on-click #(re-frame/dispatch [::auth-events/open-pin-modal user])}
   (:user/name user)])

;;
;; Shared form
;;

(defn- valid? [{:keys [name pin]} mode]
  (and (not (str/blank? name))
       (or (= mode :edit) (not (str/blank? pin)))))

(defn- user-form [{:keys [initial fields mode submit-label on-submit]}]
  (let [form (r/atom (merge {:name "" :role :member :status :active :pin ""}
                            initial))]
    (fn [{:keys [fields mode submit-label on-submit]}]
      (let [f @form]
        [:form.drawer-form {:on-submit #(.preventDefault %)}

         [:div.form-field
          [:label "Name"]
          [:input {:type        "text"
                   :placeholder "z.B. Alex Bauer"
                   :value       (:name f)
                   :on-change   #(swap! form assoc :name (.. % -target -value))}]]

         (when (= fields :admin)
           [:div.form-field
            [:label "Rolle"]
            [:select {:value     (name (:role f))
                      :on-change #(swap! form assoc :role (keyword (.. % -target -value)))}
             (for [[k label] role-labels]
               ^{:key k} [:option {:value (name k)} label])]])

         (when (= fields :admin)
           [:div.form-field
            [:label "Status"]
            [:select {:value     (name (:status f))
                      :on-change #(swap! form assoc :status (keyword (.. % -target -value)))}
             (for [[k label] status-labels]
               ^{:key k} [:option {:value (name k)} label])]])

         [:div.form-field
          [:label (if (= mode :create) "PIN"
                    [:<> "Neuer PIN" [:span.optional-hint " (leer lassen = unverändert)"]])]
          [:input {:type        "password"
                   :placeholder (when (= mode :edit) "••••")
                   :value       (:pin f)
                   :on-change   #(swap! form assoc :pin (.. % -target -value))}]]

         [:div.form-actions
          [:button.form-submit
           {:type     "submit"
            :disabled (not (valid? f mode))
            :on-click (fn [e]
                        (.preventDefault e)
                        (on-submit f))}
           submit-label]]]))))

(defn new-user-form [on-close]
  [user-form {:mode         :create
              :fields       :admin
              :submit-label "Hinzufügen"
              :on-submit    (fn [{:keys [name role pin]}]
                              (re-frame/dispatch [::events/user-create {:name name :role role :pin pin}])
                              (on-close))}])

(defn edit-user-form [user fields on-close]
  [user-form {:initial      {:name   (:user/name user)
                             :role   (:user/role user)
                             :status (:user/status user)}
              :mode         :edit
              :fields       fields
              :submit-label "Speichern"
              :on-submit    (fn [{:keys [name role status pin]}]
                              (re-frame/dispatch [::events/user-update
                                                  {:id     (:user/id user)
                                                   :name   name
                                                   :role   role
                                                   :status status
                                                   :pin    pin}])
                              (on-close))}])
