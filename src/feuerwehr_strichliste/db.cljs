(ns feuerwehr-strichliste.db
  (:require [feuerwehr-strichliste.schema :as schema]
            [feuerwehr-strichliste.domain.reducer :as reducer]
            [feuerwehr-strichliste.auth.db :as auth-db]
            [feuerwehr-strichliste.user.db :as user-db]
            [feuerwehr-strichliste.item.db :as item-db]
            [malli.core :as m]
            [malli.error :as me]
            [re-frame.core :as re-frame]))

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
