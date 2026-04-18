(ns feuerwehr-strichliste.db)

(def default-db
  {:domain {:users {1 {:user/id 1 :user/name "Alex" :user/role :admin :user/active? true}
                    2 {:user/id 2 :user/name "Maria" :user/role :kitchen :user/active? true}
                    3 {:user/id 3 :user/name "Tom" :user/role :member :user/active? true}}
            :event-log []}
   :ui     {:current-user-id nil
            :current-pin     ""}})
