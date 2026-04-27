(ns feuerwehr-strichliste.domain.reducer
  (:require [feuerwehr-strichliste.schema :as schema]
            [malli.core :as m]))

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

(defmethod reduce-event :item/updated
  [snapshot {:keys [item/id item/type item/name item/price item/stock]}]
  (update-in snapshot [:items id]
             merge {:item/type  type
                    :item/name  name
                    :item/price price
                    :item/stock stock}))

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

(defmethod reduce-event :auth/sign-in-attempted
  [snapshot _event]
  snapshot)

(defmethod reduce-event :auth/signed-out
  [snapshot _event]
  snapshot)

; build-event receives the assigned sequential id and must return a complete event — see schema/DomainEvent.
(defn apply-event [domain build-event]
  {:post [(m/validate schema/DomainEvent (:event %))]}
  (let [id      (:next-event-id domain)
        event   (build-event id)
        domain' (-> (reduce-event domain event)
                    (update :next-event-id inc))]
    {:domain domain' :event event}))
