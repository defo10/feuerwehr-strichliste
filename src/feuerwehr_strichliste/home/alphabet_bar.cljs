(ns feuerwehr-strichliste.home.alphabet-bar
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [feuerwehr-strichliste.home.subs :as subs]))

(def ^:private alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn- scroll-to-letter [letter]
  (when-let [el (.getElementById js/document (str "letter-" letter))]
    (.scrollIntoView el)))

(defn- make-observer [active-letter]
  (js/IntersectionObserver.
   (fn [entries]
     (let [visible (->> (array-seq entries)
                        (filter #(.-isIntersecting %))
                        (sort-by #(.. % -boundingClientRect -top))
                        first)]
       (when visible
         (reset! active-letter (subs (.. visible -target -id) 7)))))
   #js {:rootMargin "-10% 0px -80% 0px"}))

(defn alphabet-bar []
  (let [used          (re-frame/subscribe [::subs/used-letters])
        active-letter (r/atom nil)
        observer      (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn []
        (let [obs (make-observer active-letter)]
          (reset! observer obs)
          (doseq [letter alphabet]
            (when-let [el (.getElementById js/document (str "letter-" letter))]
              (.observe obs el)))))

      :component-will-unmount
      (fn []
        (when-let [obs @observer]
          (.disconnect obs)))

      :reagent-render
      (fn []
        (let [active   @active-letter
              used-set @used]
          [:div.alphabet-bar
           (doall (for [letter alphabet]
             (let [active?  (= letter active)
                   enabled? (contains? used-set letter)]
               [:div {:key      letter
                      :class    (str "alphabet-bar-letter"
                                     (when-not enabled? " inactive")
                                     (when active? " active"))
                      :on-click (when enabled? #(scroll-to-letter letter))}
                letter])))]))})))
