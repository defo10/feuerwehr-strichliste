(ns feuerwehr-strichliste.domain.reducer)

(def empty-snapshot
  {:users         {}
   :balances      {}
   :next-event-id 0})

(defmulti reduce-event (fn [_snapshot event] (:event/type event)))

(defmethod reduce-event :user/created
  [snapshot {:keys [event/id user/name user/role user/pin-hash]}]
  (assoc-in snapshot [:users id]
            {:user/id       id
             :user/name     name
             :user/role     role
             :user/pin-hash pin-hash
             :user/status   :active}))

(defn apply-event [domain event]
  (let [id      (:next-event-id domain)
        event'  (assoc event :event/id id)
        domain' (-> (reduce-event domain event')
                    (update :next-event-id inc))]
    {:domain domain' :event event'}))
