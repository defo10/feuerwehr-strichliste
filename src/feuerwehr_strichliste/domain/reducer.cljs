(ns feuerwehr-strichliste.domain.reducer)

(def empty-snapshot
  {:users         {}
   :balances      {}
   :items         {}
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

(defmethod reduce-event :item/created
  [snapshot {:keys [event/id
                    item/type
                    item/name
                    item/price
                    item/stock
                    item/image-key]}]
  (assoc-in snapshot [:items id]
            (cond-> {:item/id     id
                     :item/type   type
                     :item/name   name
                     :item/price  price
                     :item/stock  stock
                     :item/status :active}
              image-key (assoc :item/image-key image-key))))

(defmethod reduce-event :auth/sign-in-attempted
  [snapshot _event]
  snapshot)

(defmethod reduce-event :auth/signed-out
  [snapshot _event]
  snapshot)

; build-event receives the assigned sequential id and must return a complete event map.
(defn apply-event [domain build-event]
  (let [id      (:next-event-id domain)
        event   (build-event id)
        domain' (-> (reduce-event domain event)
                    (update :next-event-id inc))]
    {:domain domain' :event event}))
