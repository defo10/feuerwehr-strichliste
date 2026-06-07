(ns feuerwehr-strichliste.domain.storage
  (:require [konserve.core :as k]
            [konserve.indexeddb]
            [cljs.core.async :refer [go <!]]))

(def ^:private idb-config
  {:backend :indexeddb
   :id      #uuid "b2c3d4e5-f6a7-8901-bcde-f12345678901"
   :name    "feuerwehr-strichliste"})

(defonce store (atom nil))

(defn init!
  "Initializes the store and calls on-ready with persisted snapshot or nil.
   on-ready receives {:snapshot ...} if data exists, else nil."
  [on-ready]
  (go
    (if (<! (k/store-exists? idb-config {:sync? false}))
      (do
        (reset! store (<! (k/connect-store idb-config {:sync? false})))
        (let [snapshot (<! (k/get-in @store [:snapshot]))]
          (on-ready (when snapshot {:snapshot snapshot}))))
      (do
        (reset! store (<! (k/create-store idb-config {:sync? false})))
        (on-ready nil)))))

(defn clear-store! [on-done]
  (go
    (<! (k/release-store idb-config @store {:sync? false}))
    (<! (k/delete-store idb-config {:sync? false}))
    (on-done)))
