(ns feuerwehr-strichliste.session-flow-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [re-frame.core :as re-frame]
            [re-frame.db :as rf-db]
            [feuerwehr-strichliste.events :as events]
            [feuerwehr-strichliste.user.events]
            [feuerwehr-strichliste.auth.events :as auth-events]
            [feuerwehr-strichliste.item.events :as item-events]))

(def ^:private pin-hash "$2b$10$fSviXQEHvZ/dHtwvKUREbOFZcc9Recla6YM4vFMmgbLb9hyNLpij.")

(use-fixtures :each
  {:before (fn []
             (re-frame/reg-fx :persist! (fn [_] nil))
             (re-frame/reg-fx :persist-image! (fn [_] nil))
             (re-frame/reg-fx :navigate (fn [_] nil))
             (re-frame/dispatch-sync [::events/initialize-empty-db]))})

(defn- bootstrap-user! [role]
  (swap! rf-db/app-db assoc-in [:ui :current-user-id] 0)
  (re-frame/dispatch-sync [:command/create-user {:name "Test" :role role :pin-hash pin-hash}])
  (swap! rf-db/app-db assoc-in [:ui :current-user-id] nil)
  (get-in @rf-db/app-db [:domain :users 0]))

(defn- enter-pin! [user]
  (re-frame/dispatch-sync [::auth-events/open-pin-modal user])
  (doseq [digit ["1" "2" "3" "4"]]
    (re-frame/dispatch-sync [::auth-events/pin-digit digit])))

(deftest full-session-flow
  (testing "user logs in, creates an item, and logs out"
    (let [user (bootstrap-user! :admin)]
      (enter-pin! user)
      (is (some? (get-in @rf-db/app-db [:ui :current-user-id])))

      (re-frame/dispatch-sync [::item-events/item-create
                               {:item/type :drink :item/name "Cola" :item/price 150 :item/stock 10}])
      (is (some #(= "Cola" (:item/name %)) (vals (get-in @rf-db/app-db [:domain :items]))))

      (re-frame/dispatch-sync [::auth-events/sign-out])
      (is (nil? (get-in @rf-db/app-db [:ui :current-user-id]))))))
