(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]
            [feuerwehr-strichliste.domain.reducer :as reducer]
            [feuerwehr-strichliste.home.db :as home-db]))

(defn- seed-events []
  (map (fn [user]
         {:event/type      :user/created
          :event/timestamp "2026-04-18T00:00:00Z"
          :event/actor     0
          :user/name       (:user/name user)
          :user/role       (:user/role user)
          :user/pin-hash   (:user/pin-hash user)})
       (vals (schema/generate-users 5))))

(def default-db
  {:domain (reduce (fn [domain event]
                     (:domain (reducer/apply-event domain #(assoc event :event/id %))))
                   reducer/empty-snapshot
                   (seed-events))
   :ui     home-db/default-ui})
