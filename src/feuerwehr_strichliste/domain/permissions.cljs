(ns feuerwehr-strichliste.domain.permissions)

(def ^:private capabilities
  {:kitchen #{:restock :manage-items}
   :admin   #{:manage-users :view-all-profiles :void-transaction :manage-items :confirm-top-ups}})

(defn- restricted? [capability]
  (some #(contains? % capability) (vals capabilities)))

(defn can? [role capability]
  (or (not (restricted? capability))
      (contains? (get capabilities role) capability)))
