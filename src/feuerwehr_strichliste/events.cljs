(ns feuerwehr-strichliste.events
  (:require
   [re-frame.core :as re-frame]
   [feuerwehr-strichliste.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::initialize-empty-db
 (fn-traced [_ _]
            db/empty-db))

(re-frame/reg-event-db
 ::initialize-from-storage
 (fn-traced [_ [_ {:keys [snapshot]}]]
            (assoc db/empty-db :snapshot snapshot)))

(re-frame/reg-event-fx
 ::load-activity-log
 (fn-traced [_ _]
            {:load-activity-log! nil}))

(re-frame/reg-event-db
 :activity-log/loaded
 (fn-traced [db [_ event-log]]
            (assoc-in db [:ui :activity-log] event-log)))

(re-frame/reg-event-fx
 ::navigate
 (fn-traced [_ [_ handler]]
            {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel]]
            {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-db
 :error
 (fn-traced [db [_ type message]]
            (assoc-in db [:ui :error] {:type type :message message})))

(re-frame/reg-event-db
 :error/dismiss
 (fn-traced [db _]
            (assoc-in db [:ui :error] nil)))

