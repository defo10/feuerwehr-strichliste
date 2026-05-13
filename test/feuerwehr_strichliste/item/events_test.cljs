(ns feuerwehr-strichliste.item.events-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [re-frame.core :as re-frame]
            [re-frame.db :as rf-db]
            [feuerwehr-strichliste.events :as events]
            [feuerwehr-strichliste.user.events]
            [feuerwehr-strichliste.item.events :as item-events]))

(use-fixtures :each
  {:before (fn []
             (re-frame/reg-fx :persist! (fn [_] nil))
             (re-frame/reg-fx :persist-image! (fn [_] nil))
             (re-frame/dispatch-sync [::events/initialize-empty-db]))})

(defn- create-user! [role]
  (swap! rf-db/app-db assoc-in [:ui :current-user-id] "system")
  (re-frame/dispatch-sync [:command/create-user {:name "Test" :role role :pin-hash "hash"}])
  (let [user-id (-> @rf-db/app-db (get-in [:snapshot :users]) keys first)]
    (swap! rf-db/app-db assoc-in [:ui :current-user-id] user-id)
    user-id))

(def ^:private test-item
  {:item/type :drink :item/name "Test Cola" :item/price 150 :item/stock 10})

(deftest admin-can-create-item
  (testing "admin can add a new item"
    (create-user! :admin)
    (re-frame/dispatch-sync [::item-events/item-create test-item])
    (is (some #(= "Test Cola" (:item/name %))
              (vals (get-in @rf-db/app-db [:snapshot :items]))))))

(deftest member-cannot-create-item
  (testing "member is rejected and no item is added"
    (create-user! :member)
    (re-frame/dispatch-sync [::item-events/item-create test-item])
    (is (empty? (get-in @rf-db/app-db [:snapshot :items])))))

(deftest checkout-deducts-balance-and-stock
  (testing "selecting an item and confirming receipt deducts balance and stock"
    (let [user-id (create-user! :admin)]
      (re-frame/dispatch-sync [::item-events/item-create
                               {:item/type :drink :item/name "Cola" :item/price 150 :item/stock 10}])
      (let [item-id (-> @rf-db/app-db (get-in [:snapshot :items]) keys first)]
        (re-frame/dispatch-sync [::item-events/increment item-id])
        (re-frame/dispatch-sync [::item-events/show-receipt])
        (re-frame/dispatch-sync [::item-events/confirm-checkout])
        (let [db @rf-db/app-db]
          (is (= -150 (get-in db [:snapshot :balances user-id])))
          (is (= 9 (get-in db [:snapshot :items item-id :item/stock]))))))))
