(ns bluegenes.tools.datamerger.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [reagent.core :as reagent]
            [bluegenes.components.queryoperations.view :as venn]
            [bluegenes.utils.maths :as maths]
            [com.rpl.specter :as s]
            [bluegenes.utils.imcljs :as im]
            [clojure.set :as set :refer [union intersection difference]]
            [cljs.core.async :as a :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def width 500)


(def anchor1 {:x (- (* .33 width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 width) (/ width 2)) :y 0})
(def radius (* .33 width))

(def height (* 2 radius))


(def center {:x 250 :y 250})
;(def radius 150)

(defn dropdown-research []
  (let [saved-research (subscribe [:saved-research])
        lists          (subscribe [:lists])]
    (fn [position]
      [:div
       [:div.btn-group
        [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
         [:span (str "Choose Query" (:label @saved-research)) [:span.caret]]]
        [:div.dropdown-menu.scrollable-menu
         [:h4 "My Saved Queries"]
         [:ul.list-unstyled
          (for [[id details] @saved-research]
            (do
              [:li
               {:on-click (fn [] (re-frame/dispatch [:set-qop position id :saved-data]))}
               [:a (:label details)]]))]
         [:h4 "Public Queries"]
         [:ul.list-unstyled
          (for [details (:flymine @lists)]
            (do
              [:li
               {:on-click (fn [] (re-frame/dispatch [:set-qop position (:name details) :list :flymine]))}
               [:a (:title details)]]))]]]])))


(defn list-group-item []
  (fn [details {{:keys [update-state]} :api state :state}]
    ;(println "details" details)
    [:a.list-group-item
     {:on-click (partial update-state #(assoc-in % [:c2 :list-name] (:name details)))}
     [:div.list-group-item-heading (:title details)]
     [:p "Some description of the data"]]))

(defn researchlist []
  (let [saved-research (subscribe [:saved-research])
        lists          (subscribe [:lists])]
    (fn [step-data]
      (into [:div.list-group.clip-and-scroll-333.skinny] (map (fn [x] [list-group-item x step-data]) (:flymine @lists))))))


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
  (fn [{{:keys [update-state]} :api state :state cache :cache}]
    [:g
     [:circle.circle2
      {:r        radius
       :class    (if (-> state :c2 :keep) "on")
       :on-click (partial update-state #(assoc-in % [:c2 :keep] (not (:keep (:c2 %)))))}]
     [:text.venn-count {:text-anchor "middle" :dx 100} (-> cache :c2 :count)]
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
     [:g
      ;{:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
      {:transform (str "translate(" (/ width 2) "," radius ")")}
      [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circle1 step-data]]
      [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circle2 step-data]]
      [overlap step-data]]]))

(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [step-data]
                       [:div
                        ;[:h1 "Data Merge"]
                        [:div.container
                         [:div.row
                          [:div.col-sm-3 [:span "hi"]]
                          [:div.col-sm-6 [svg step-data]]
                          [:div.col-sm-3 [researchlist step-data]]]]
                        [:div "input"]
                        [:div (str (:input step-data))]
                        [:div "state"]
                        [:div (str (:state step-data))]
                        [:div "cache"]
                        [:div (str (:cache step-data))]])}))


(def missing? (complement contains?))

(def present? (complement contains?))

(defn meld-query-results [op s1 s2]
  (println "MELDING")
  (println "op" op)
  (println "s1" (count s1) s1)
  (println "s2" (count s2) s2)
  {:from   "Gene"
   :select "*"
   :where  [{:path   "Gene.id"
             :op     "ONE OF"
             :values (case op
                       "union" (union s1 s2)
                       "intersection" (intersection s1 s2)
                       "asymmetric-left" (difference s1 s2)
                       "asymmetric-right" (difference s2 s1)
                       "subtract" (union (difference s1 s2) (difference s2 s1)))}]})

(defn convert-list-to-query [list-name]
  {:select "Gene.id"
   :from   "Gene"
   :where  [{:path  "Gene"
             :op    "IN"
             :value list-name}]})

(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [{:keys [cache state] :as snapshot}
   what-changed
   {:keys [has-something update-cache update-state] :as api}
   global-cache]

  ; Clear the cache if the input changes
  (if (s/select-one [:input] what-changed)
    (update-cache (fn [cache]
                    (assoc-in cache [:c1 :query] (assoc (:payload (:data (:input snapshot))) :select "Genes.homologues.homologue.id")))))

  (if (s/select-one [:cache :c2 :query] what-changed)
    (do
      (println "clearing query")
      (update-cache (fn [cache] (assoc-in cache [:c2 :count] nil)))))

  ; Have any of the keep/not-keep statuses changed?
  (if (not-empty (s/select [:state s/ALL s/LAST :keep] what-changed))
    (let [update-operator (fn [op] (update-state (fn [state] (assoc state :operator op))))]
      (-> (cond
            (and (true? (-> state :c1 :keep))
                 (true? (-> state :c2 :keep))
                 (true? (-> state :center :keep))) "union"
            (and (true? (-> state :c1 :keep))
                 (true? (-> state :c2 :keep))
                 (not (true? (-> state :center :keep)))) "subtract"
            (and (not (true? (-> state :c1 :keep)))
                 (= true (-> state :c2 :keep))
                 (not (= true? (-> state :center :keep)))) "asymmetric-right"
            (and (= true (-> state :c1 :keep))
                 (not (true? (-> state :c2 :keep)))
                 (not (true? (-> state :center :keep)))) "asymmetric-left"
            (and (not (true? (-> state :c1 :keep)))
                 (not (true? (-> state :c2 :keep)))
                 (= true (-> state :center :keep))) "intersection")
          update-operator)))


  (println "RES" (s/select-one [:cache s/ALL s/LAST :query] what-changed))

  ; If operator has changed then we can run our query
  (if-let [res (s/select-one [:cache s/ALL s/LAST :query] what-changed)]
    (println "NEED TO RUN AND REPORT HAS SOMETHING" what-changed)
    (go (let [c1-result (into #{} (flatten (:results (<! (im/raw-query-rows {:root "www.flymine.org/query"}
                                                                   (get-in snapshot [:cache :c1 :query])
                                                                   {:format "json"})))))
              c2-result (into #{} (flatten (:results (<! (im/raw-query-rows {:root "www.flymine.org/query"}
                                                                   (get-in snapshot [:cache :c2 :query])
                                                                   {:format "json"})))))]

          (has-something
            {:data     {:format  "query"
                        :type    "Gene"
                        :payload (meld-query-results (get-in snapshot [:state :operator]) c1-result c2-result)}
             :service  {:root "www.flymine.org/query"}} ))))

  ; If user selects a new list then convert it to a query and
  ; save it to the cache
  (if-let [list-name (s/select-one [:state :c2 :list-name] what-changed)]
    (update-cache (fn [cache]
                    (assoc-in cache [:c2 :query] (convert-list-to-query list-name)))))

  ; Count our input query there's no count in the cache
  (if (s/select-one [:input] what-changed)
    (go (let [c (<! (im/query-count {:root "www.flymine.org/query"}
                                    (:payload (:data (:input snapshot)))))]
          (update-cache (fn [cache] (assoc-in cache [:c1 :count] c))))))

  (if-not (s/select-one [:c2 :count] cache)
    (go (let [c (<! (im/query-count {:root "www.flymine.org/query"}
                                    (get-in snapshot [:cache :c2 :query])))]
          (update-cache (fn [cache] (assoc-in cache [:c2 :count] c)))))))


