(ns feuerwehr-strichliste.storage
  (:require [konserve.core :as k]
            [konserve.indexeddb]
            [cljs.core.async :refer [go <!]]
            [feuerwehr-strichliste.config :as config]))

(def ^:private mem-config
  {:backend :memory
   :id      #uuid "a1b2c3d4-e5f6-7890-abcd-ef1234567890"})

(def ^:private idb-config
  {:backend :indexeddb
   :id      #uuid "b2c3d4e5-f6a7-8901-bcde-f12345678901"
   :name    "feuerwehr-strichliste"})

(defonce store (atom nil))

(defn init! [on-ready]
  (if config/debug?
    (do (reset! store (k/create-store mem-config {:sync? true}))
        (on-ready))
    (go
      (reset! store (<! (k/create-store idb-config {:sync? false})))
      (on-ready))))
