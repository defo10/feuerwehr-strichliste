(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]
            [feuerwehr-strichliste.domain.reducer :as reducer]
            [feuerwehr-strichliste.auth.db :as auth-db]
            [feuerwehr-strichliste.user.db :as user-db]
            [feuerwehr-strichliste.item.db :as item-db]
            [malli.core :as m]
            [malli.error :as me]
            [re-frame.core :as re-frame]
            ; for dev:
            [feuerwehr-strichliste.domain.storage :as storage]
            [feuerwehr-strichliste.user.events :as user-events]
            [re-frame.db :as rf-db]))

(def check-schema-interceptor
  (re-frame/after
   (fn [db]
     (when-not (m/validate schema/AppDb db)
       (throw (ex-info (str "db schema check failed: "
                            (me/humanize (m/explain schema/AppDb db)))
                       {}))))))

(def ^:private default-ui
  (merge auth-db/default-ui user-db/default-ui item-db/default-ui))

(def empty-db
  {:snapshot     reducer/empty-snapshot
   :ui           default-ui
   :active-panel :home-panel})

(comment
  ;; REPL setup: (shadow.cljs.devtools.api/nrepl-select :app)

  ;; clear local data:
  (storage/clear-store! (fn [] (.reload js/location)))

  (.reload js/location)

  (do
    ;; 1. Create an admin user (bypasses permission checks) 
    (re-frame.core/dispatch-sync
     [:command/create-user
      {:name     "Admin"
       :role     :admin
       :pin-hash (user-events/hash-pin "1234")}])

    ;; 2. Sign in as that user via the real auth flow
    (let [user (first (vals (get-in @rf-db/app-db [:snapshot :users])))]
      (re-frame.core/dispatch-sync [:feuerwehr-strichliste.auth.events/open-pin-modal user]))
    (doseq [d ["1" "2" "3" "4"]]
      (re-frame.core/dispatch-sync [:feuerwehr-strichliste.auth.events/pin-digit d])))

  ;; 3. Create more users
  (re-frame.core/dispatch-sync
   [:feuerwehr-strichliste.user.events/user-create
    {:name "Max Mustermann" :role :member :pin "5678"}])

  ;; 4. Create items
  (re-frame.core/dispatch-sync
   [:feuerwehr-strichliste.item.events/item-create
    {:item/type :drink :item/name "Kaffee" :item/price 150 :item/stock 50}])

  (re-frame.core/dispatch-sync
   [:feuerwehr-strichliste.item.events/item-create
    {:item/type :food :item/name "Brötchen" :item/price 80 :item/stock 20}]))
