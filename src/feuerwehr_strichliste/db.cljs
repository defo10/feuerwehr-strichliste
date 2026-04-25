(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]
            [feuerwehr-strichliste.domain.reducer :as reducer]
            [feuerwehr-strichliste.auth.db :as auth-db]
            [feuerwehr-strichliste.user.db :as user-db]
            [feuerwehr-strichliste.item.db :as item-db]))

(def ^:private seed-items
  [{:item/type :drink :item/name "Kaffee"     :item/price 150 :item/stock 50}
   {:item/type :drink :item/name "Wasser"     :item/price  80 :item/stock 100}
   {:item/type :drink :item/name "Cola"       :item/price 150 :item/stock 24}
   {:item/type :drink :item/name "Apfelsaft"  :item/price 120 :item/stock 24}
   {:item/type :drink :item/name "Bier"       :item/price 200 :item/stock 20}
   {:item/type :food  :item/name "Brötchen"   :item/price  80 :item/stock 10}
   {:item/type :food  :item/name "Snickers"   :item/price 150 :item/stock 15}
   {:item/type :food  :item/name "Chips"      :item/price 100 :item/stock 10}])

(defn- seed-events []
  (concat
   (map (fn [user]
          {:event/type      :user/created
           :event/timestamp "2026-04-18T00:00:00Z"
           :event/actor     0
           :user/name       (:user/name user)
           :user/role       (:user/role user)
           :user/pin-hash   (:user/pin-hash user)})
        (vals (schema/generate-users 5)))
   (map (fn [item]
          (merge {:event/type      :item/created
                  :event/timestamp "2026-04-18T00:00:00Z"
                  :event/actor     0}
                 item))
        seed-items)))

(def empty-db
  {:domain reducer/empty-snapshot
   :ui     (merge auth-db/default-ui user-db/default-ui item-db/default-ui)})

(def default-db
  {:domain (reduce (fn [domain event]
                     (:domain (reducer/apply-event domain #(assoc event :event/id %))))
                   reducer/empty-snapshot
                   (seed-events))
   :ui     (merge auth-db/default-ui user-db/default-ui item-db/default-ui)})
