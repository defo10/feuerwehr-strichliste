(ns feuerwehr-strichliste.domain.reducer
  (:require [feuerwehr-strichliste.schema :as schema]
            ["uuid" :refer [v7]]
            [malli.core :as m]))

(defn- append-history-entry! [history entry]
  (m/assert schema/UserHistory (conj history entry)))

(defn- update-history-entry! [history entry-id updates]
  (m/assert schema/UserHistory
            (mapv #(if (= entry-id (:history/id %))
                     (merge % updates)
                     %)
                  history)))

(def empty-snapshot
  {:users    {}
   :balances {}
   :items    {}})

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
             :user/status   :active
             :user/history  []}))

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

(defmethod reduce-event :item/stock-corrected
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
  [snapshot {:keys [event/id event/actor event/subject event/timestamp checkout/entries]}]
  (let [uid (or subject actor)]
    (-> (reduce (fn [snap {:keys [item-id quantity unit-price]}]
                  (-> snap
                      (update-in [:balances uid] (fnil - 0) (* quantity unit-price))
                      (update-in [:items item-id :item/stock] - quantity)))
                snapshot
                entries)
        (update-in [:users uid :user/history] append-history-entry!
                   {:history/id        id
                    :history/type      :checkout
                    :history/timestamp timestamp
                    :history/actor     actor
                    :history/status    :active
                    :checkout/entries  entries}))))

(defmethod reduce-event :balance/top-up-requested
  [snapshot {:keys [event/id event/actor event/subject event/timestamp top-up/amount] :as event}]
  (let [uid (or subject (:top-up/user-id event))]
    (-> snapshot
        (update-in [:balances uid] (fnil + 0) amount)
        (update-in [:users uid :user/history] append-history-entry!
                   {:history/id        id
                    :history/type      :top-up
                    :history/timestamp timestamp
                    :history/actor     actor
                    :history/status    :active
                    :top-up/amount     amount}))))

(defmethod reduce-event :balance/top-up-confirmed
  [snapshot {:keys [event/subject event/actor event/timestamp top-up/request-id]}]
  (update-in snapshot [:users subject :user/history]
             update-history-entry! request-id
             {:history/status  :confirmed
              :top-up/reviewed-at timestamp
              :top-up/reviewed-by actor}))

(defmethod reduce-event :balance/top-up-cancelled
  [snapshot {:keys [event/subject event/actor event/timestamp top-up/request-id top-up/amount] :as event}]
  (let [uid (or subject (:top-up/user-id event))]
    (-> snapshot
        (update-in [:balances uid] - amount)
        (update-in [:users uid :user/history]
                   update-history-entry! request-id
                   {:history/status  :cancelled
                    :top-up/reviewed-at timestamp
                    :top-up/reviewed-by actor}))))

(defmethod reduce-event :transaction/voided
  [snapshot {:keys [event/subject void/original-id checkout/entries]}]
  (-> (reduce (fn [snap {:keys [item-id quantity unit-price]}]
                (-> snap
                    (update-in [:balances subject] (fnil + 0) (* quantity unit-price))
                    (update-in [:items item-id :item/stock] + quantity)))
              snapshot
              entries)
      (update-in [:users subject :user/history]
                 update-history-entry! original-id {:history/status :voided})))

(defmethod reduce-event :auth/sign-in-attempted
  [snapshot _event]
  snapshot)

(defmethod reduce-event :auth/signed-out
  [snapshot _event]
  snapshot)

; build-event receives the assigned UUIDv7 id and must return a complete event — see schema/DomainEvent.
(defn apply-event [snapshot build-event]
  {:post [(m/validate schema/DomainEvent (:event %))]}
  (let [id        (v7)
        event     (build-event id)
        snapshot' (reduce-event snapshot event)]
    {:snapshot snapshot' :event event}))
