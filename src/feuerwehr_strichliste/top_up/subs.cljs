(ns feuerwehr-strichliste.top-up.subs
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as app-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

; Scans the full event log on every change. An intermediate filtered-events sub would avoid
; unnecessary re-runs, but this sub is only active on the top-ups page so the cost is acceptable.
(re-frame/reg-sub
 ::top-ups-map
 :<- [::app-subs/event-log]
 (fn [event-log _]
   (reduce (fn [m event]
             (case (:event/type event)
               :balance/top-up-requested
               (assoc m (:event/id event)
                      {:top-up/id           (:event/id event)
                       :top-up/user-id      (or (:event/subject event) (:top-up/user-id event))
                       :top-up/amount       (:top-up/amount event)
                       :top-up/requested-at (:event/timestamp event)
                       :top-up/requested-by (:event/actor event)
                       :top-up/status       :pending})
               :balance/top-up-confirmed
               (update m (:top-up/request-id event) merge
                       {:top-up/status       :confirmed
                        :top-up/confirmed-at (:event/timestamp event)
                        :top-up/confirmed-by (:event/actor event)})
               :balance/top-up-cancelled
               (update m (:top-up/request-id event) merge
                       {:top-up/status       :cancelled
                        :top-up/cancelled-at (:event/timestamp event)
                        :top-up/cancelled-by (:event/actor event)})
               m))
           {}
           event-log)))

(re-frame/reg-sub
 ::top-ups-sorted
 :<- [::top-ups-map]
 (fn [top-ups-map _]
   (sort-by (juxt #(if (= :pending (:top-up/status %)) 0 1)
                  #(- (.getTime (js/Date. (:top-up/requested-at %)))))
            (vals top-ups-map))))

(re-frame/reg-sub
 ::can-confirm-top-ups?
 :<- [::app-subs/current-user]
 (fn [user _]
   (permissions/can? (:user/role user) :confirm-top-ups)))

(re-frame/reg-sub
 ::pending-top-up
 (fn [db _] (get-in db [:ui :pending-top-up])))

(re-frame/reg-sub
 ::top-up-editing?
 (fn [db _] (get-in db [:ui :top-up-editing?])))
