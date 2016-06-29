(ns bluegenes.tools.datamerger.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.components.queryoperations.view :as venn]
            [bluegenes.utils.maths :as maths]
            [com.rpl.specter :as s]
            [bluegenes.utils.imcljs :as im]
            [cljs.core.async :as a :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def width 500)
(def height 500)

(def anchor1 {:x (- (* .33 width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 width) (/ width 2)) :y 0})
(def radius (* .33 width))


(def center {:x 250 :y 250})
;(def radius 150)


(defn overlap []
  (let [intersection-points (maths/circle-intersections (:x anchor1) 0 radius (:x anchor2) 0 radius)
        keep-middle         (re-frame/subscribe [:qop-middle])]
    (fn [{{:keys [update-state]} :api, state :state}]
      [:path.overlap {:on-click (partial update-state #(assoc-in % [:center :keep] (not (:keep (:center %)))))
                      :class    (if (-> state :center :keep) "on")
                      :d        (clojure.string/join
                                  " "
                                  ["M" (nth intersection-points 1) (nth intersection-points 3)
                                   "A" radius radius 0 0 1
                                   (nth intersection-points 0) (nth intersection-points 2)
                                   "A" radius radius 0 0 1
                                   (nth intersection-points 1) (nth intersection-points 3)
                                   "Z"])}])))

(defn circle2 []
  (fn [{{:keys [update-state]} :api state :state}]
    [:g
     [:circle.circle2
      {:r        radius
       :class    (if (-> state :c2 :keep) "on")
       :on-click (partial update-state #(assoc-in % [:c2 :keep] (not (:keep (:c2 %)))))}]
     [:text.venn-count {:text-anchor "middle" :dx 100} "114"]
     [:text.venn-label {:text-anchor "middle" :dx 100 :dy "1em"} "Genes"]]))

(defn circle1 []
  (fn [{{:keys [update-state]} :api state :state cache :cache}]
    [:g
     [:circle.circle1
      {:r        radius
       :class    (if (-> state :c1 :keep) "on")
       :on-click (partial update-state #(assoc-in % [:c1 :keep] (not (:keep (:c1 %)))))}]
     [:text.venn-count {:text-anchor "middle" :dx -100} (-> cache :c1 :count)]
     [:text.venn-label {:text-anchor "middle" :dx -100 :dy "1em"} "Genes"]]))

(defn svg []
  (fn [step-data]
    [:svg.venn {:width width :height height}
     [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
      [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circle1 step-data]]
      [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circle2 step-data]]
      [overlap step-data]]]))



(def missing? (complement contains?))

(def present? (complement contains?))




(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [{:keys [cache state] :as snapshot}
   what-changed
   {:keys [has-something update-cache update-state] :as api}
   global-cache]


  ; Clear the cache if the input changes
  (if (s/select-one [:input] what-changed)
    (update-cache (fn [cache] (assoc-in cache [:c1 :count] nil))))

  ; Have any of the keep/not-keep statuses changed?
  (if (not-empty (s/select [:state s/ALL s/LAST :keep] what-changed))
    (let [update-operator (fn [op] (update-state (fn [state] (assoc state :operator op))))]
      (-> (cond
            (and (= true (-> state :c1 :keep))
                 (= true (-> state :c2 :keep))
                 (= true (-> state :center :keep))) "union"
            (and (= true (-> state :c1 :keep))
                 (= true (-> state :c2 :keep))
                 (= false (-> state :center :keep))) "subtract"
            (and (= false (-> state :c1 :keep))
                 (= true (-> state :c2 :keep))
                 (= false (-> state :center :keep))) "asymmetric-right"
            (and (= true (-> state :c1 :keep))
                 (= false (-> state :c2 :keep))
                 (= false (-> state :center :keep))) "asymmetric-left"
            (and (= false (-> state :c1 :keep))
                 (= false (-> state :c2 :keep))
                 (= true (-> state :center :keep))) "intersection")
          update-operator)))

  ; If operator has changed then we can run our query
  (if (s/select-one [:state :operator] what-changed)
    (println "NEED TO RUN" what-changed))

  ; Count our input query there's no count in the cache
  (if-not (s/select-one [:c1 :count] cache)
    (go (let [c (<! (im/query-count {:root "www.flymine.org/query"}
                                    (:payload (:data (:input snapshot)))))]
          (update-cache (fn [cache] (assoc-in cache [:c1 :count] c)))))))




(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [step-data]
                       [:div
                        [:h1 "Banana Caker"]
                        [:div (str (:input step-data))]
                        [:div (str (:state step-data))]
                        [:div (str (:cache step-data))]
                        [svg step-data]])}))

