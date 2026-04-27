(ns feuerwehr-strichliste.item.subs
  (:require [re-frame.core :as re-frame]
            [feuerwehr-strichliste.subs :as app-subs]
            [feuerwehr-strichliste.domain.permissions :as permissions]))

(re-frame/reg-sub
 ::cart
 (fn [db _]
   (get-in db [:ui :cart])))

(re-frame/reg-sub
 ::active-tab
 (fn [db _]
   (get-in db [:ui :active-tab])))

(re-frame/reg-sub
 ::receipt
 (fn [db _]
   (get-in db [:ui :receipt])))

(re-frame/reg-sub
 ::cart-has-items?
 :<- [::cart]
 (fn [cart _]
   (some pos? (vals cart))))

(re-frame/reg-sub
 ::cart-qty
 (fn [_ _] (re-frame/subscribe [::cart]))
 (fn [cart [_ item-id]]
   (get cart item-id 0)))

(re-frame/reg-sub
 ::editing-item
 (fn [db _]
   (get-in db [:ui :editing-item])))

(re-frame/reg-sub
 ::can-manage-items?
 :<- [::app-subs/current-user]
 (fn [user _]
   (permissions/can? (:user/role user) :manage-items)))

(re-frame/reg-sub
 ::items-map
 (fn [db _]
   (get-in db [:domain :items])))

(re-frame/reg-sub
 ::item-images
 (fn [db _]
   (get-in db [:ui :item-images])))

(re-frame/reg-sub
 ::item-image-url
 :<- [::item-images]
 (fn [images [_ item-id]]
   (get images item-id)))

(re-frame/reg-sub
 ::items
 :<- [::items-map]
 (fn [items-map _]
   (->> items-map
        vals
        (filter #(= :active (:item/status %)))
        (sort-by :item/name))))

(re-frame/reg-sub
 ::items-by-type
 :<- [::items]
 (fn [items _]
   (group-by :item/type items)))

(re-frame/reg-sub
 ::cart-total
 :<- [::cart]
 :<- [::items-map]
 (fn [[cart items-map] _]
   (reduce (fn [sum [item-id qty]]
             (+ sum (* qty (get-in items-map [item-id :item/price] 0))))
           0
           cart)))
