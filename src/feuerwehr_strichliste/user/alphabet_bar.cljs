(ns feuerwehr-strichliste.user.alphabet-bar
  (:require [reagent.core :as r]))

(def ^:private alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn- scroll-to-letter [id-prefix letter]
  (when-let [el (.getElementById js/document (str id-prefix letter))]
    (.scrollIntoView el)))

(defn- make-observer [active-letter prefix-len]
  (js/IntersectionObserver.
   (fn [entries]
     (let [visible (->> (array-seq entries)
                        (filter #(.-isIntersecting %))
                        (sort-by #(.. % -boundingClientRect -top))
                        first)]
       (when visible
         (reset! active-letter (subs (.. visible -target -id) prefix-len)))))
   #js {:rootMargin "-10% 0px -80% 0px"}))

(defn alphabet-bar [{:keys [used-letters id-prefix]}]
  (let [active-letter (r/atom nil)
        observer      (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [{:keys [id-prefix]} (r/props this)
              obs (make-observer active-letter (count id-prefix))]
          (reset! observer obs)
          (doseq [letter alphabet]
            (when-let [el (.getElementById js/document (str id-prefix letter))]
              (.observe obs el)))))

      :component-will-unmount
      (fn []
        (when-let [obs @observer]
          (.disconnect obs)))

      :reagent-render
      (fn [{:keys [used-letters id-prefix top]}]
        (let [active @active-letter]
          [:div.alphabet-bar (when top {:style {:top top}})
           (doall (for [letter alphabet]
                    (let [active?  (= letter active)
                          enabled? (contains? used-letters letter)]
                      [:div {:key      letter
                             :class    (str "alphabet-bar-letter"
                                            (when-not enabled? " inactive")
                                            (when active? " active"))
                             :on-click (when enabled? #(scroll-to-letter id-prefix letter))}
                       letter])))]))})))
