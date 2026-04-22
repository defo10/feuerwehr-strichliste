(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]
            [feuerwehr-strichliste.domain.reducer :as reducer]))

(defn- seed-events []
  (map (fn [user]
         {:event/type      :user/created
          :event/timestamp "2026-04-18T00:00:00Z"
          :user/name       (:user/name user)
          :user/role       (:user/role user)
          :user/pin-hash   (:user/pin-hash user)})
       (vals (schema/generate-users 20))))

(def default-db
  {:domain (reduce #(:domain (reducer/apply-event %1 %2)) reducer/empty-snapshot (seed-events))
   :ui     {:current-user-id nil
            :pin             {:user nil :digits "" :error nil :success false}
            :search-query    ""}})
