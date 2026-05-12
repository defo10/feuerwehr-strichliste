(ns feuerwehr-strichliste.domain.reducer
  (:require [feuerwehr-strichliste.schema :as schema]
            [malli.core :as m]))

(def empty-snapshot
  {:users         {}
   :balances      {}
   :items         {}
   :top-ups       {}
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
  [snapshot {:keys [event/id event/actor event/timestamp top-up/user-id top-up/amount]}]
  (-> snapshot
      (update-in [:balances user-id] (fnil + 0) amount)
      (assoc-in [:top-ups id]
                {:top-up/id           id
                 :top-up/user-id      user-id
                 :top-up/amount       amount
                 :top-up/requested-at timestamp
                 :top-up/requested-by actor
                 :top-up/status       :pending})))

(defmethod reduce-event :balance/top-up-confirmed
  [snapshot {:keys [event/actor event/timestamp top-up/request-id]}]
  (update-in snapshot [:top-ups request-id] merge
             {:top-up/status       :confirmed
              :top-up/confirmed-at timestamp
              :top-up/confirmed-by actor}))

(defmethod reduce-event :balance/top-up-cancelled
  [snapshot {:keys [event/actor event/timestamp top-up/request-id]}]
  (let [{:top-up/keys [amount user-id]} (get-in snapshot [:top-ups request-id])]
    (-> snapshot
        (update-in [:balances user-id] - amount)
        (update-in [:top-ups request-id] merge
                   {:top-up/status       :cancelled
                    :top-up/cancelled-at timestamp
                    :top-up/cancelled-by actor}))))

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
