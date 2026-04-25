(ns feuerwehr-strichliste.item.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::items-map
 (fn [db _]
   (get-in db [:domain :items])))

(re-frame/reg-sub
 ::items
 :<- [::items-map]
 (fn [items-map _]
   (->> items-map
        vals
        (filter #(= :active (:item/status %)))
        (sort-by :item/name))))
