(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]))

(def default-db
  {:domain {:users     (schema/generate-users 500)
            :event-log []}
   :ui     {:current-user-id nil
            :current-pin     ""
            :search-query    ""}})
