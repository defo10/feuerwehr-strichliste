(ns feuerwehr-strichliste.domain.reducer
  (:require [feuerwehr-strichliste.schema :as schema]
            [malli.core :as m]))

(def empty-snapshot
  {:users         {}
   :balances      {}
   :items         {}
   :next-event-id 0})

(defn- merge-non-nil [target updates]
  (reduce-kv (fn [m k v] (if (nil? v) m (assoc m k v))) target updates))

(defmulti reduce-event (fn [_snapshot event] (:event/type event)))

(defmethod reduce-event :user/updated
  [snapshot {:keys [user/id user/name user/role user/status user/pin-hash]}]
  (cond-> (update-in snapshot [:users id] merge {:user/name   name
                                                  :user/role   role
                                                  :user/status status})
    pin-hash (assoc-in [:users id :user/pin-hash] pin-hash)))

(defmethod reduce-event :user/created
  [snapshot {:keys [event/id user/name user/role user/pin-hash]}]
  (assoc-in snapshot [:users id]
            {:user/id       id
             :user/name     name
             :user/role     role
             :user/pin-hash pin-hash
             :user/status   :active}))

(defmethod reduce-event :item/edited
  [snapshot {:keys [item/id item/type item/name item/price item/image-key]}]
  (update-in snapshot [:items id]
             merge-non-nil {:item/type      type
                            :item/name      name
                            :item/price     price
                            :item/image-key image-key}))

(defmethod reduce-event :item/restocked
  [snapshot {:keys [item/id item/stock]}]
  (assoc-in snapshot [:items id :item/stock] stock))

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

(defmethod reduce-event :cart/checked-out
  [snapshot {:keys [event/actor checkout/entries]}]
  (reduce (fn [snap {:keys [item-id quantity unit-price]}]
            (-> snap
                (update-in [:balances actor] (fnil - 0) (* quantity unit-price))
                (update-in [:items item-id :item/stock] - quantity)))
          snapshot
          entries))

(defmethod reduce-event :balance/top-up-requested
  [snapshot {:keys [top-up/user-id top-up/amount]}]
  (update-in snapshot [:balances user-id] (fnil + 0) amount))

(defmethod reduce-event :balance/top-up-confirmed
  [snapshot _event]
  snapshot)

(defmethod reduce-event :balance/top-up-cancelled
  [snapshot {:keys [top-up/user-id top-up/amount]}]
  (update-in snapshot [:balances user-id] - amount))

(defmethod reduce-event :auth/sign-in-attempted
  [snapshot _event]
  snapshot)

(defmethod reduce-event :auth/signed-out
  [snapshot _event]
  snapshot)

; build-event receives the assigned sequential id and must return a complete event — see schema/DomainEvent.
(defn apply-event [snapshot build-event]
  {:post [(m/validate schema/DomainEvent (:event %))]}
  (let [id        (:next-event-id snapshot)
        event     (build-event id)
        snapshot' (-> (reduce-event snapshot event)
                      (update :next-event-id inc))]
    {:snapshot snapshot' :event event}))
