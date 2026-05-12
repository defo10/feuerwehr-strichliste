(ns feuerwehr-strichliste.domain.storage
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

(defn init!
  "Initializes the store and calls on-ready with persisted data when available.
   In dev mode, on-ready receives nil (use seed data).
   In prod, on-ready receives {:snapshot ... :event-log ...} if data exists, else nil."
  [on-ready]
  (if config/debug?
    (do (reset! store (k/create-store mem-config {:sync? true}))
        (on-ready nil))
    (go
      (reset! store (<! (k/create-store idb-config {:sync? false})))
      (let [snapshot  (<! (k/get-in @store [:snapshot]))
            event-log (<! (k/get-in @store [:event-log]))]
        (on-ready (when snapshot
                    {:snapshot  snapshot
                     :event-log (or event-log [])}))))))
