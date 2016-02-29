(ns bluegenes.tools.enrichment.core
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.components.paginator :as paginator]
            [reagent.impl.util :as impl :refer [extract-props]]))

(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(def pager (reagent/atom {:partition 10
                  :current-page 1}))

(defn enrichment-controls []
  (.log js/console "d3 is" js/d3)
  [:div
  [:form.form-group
   [:label "Maximum p-value:"]
   [:input {:type "text"}]]
  [:form.form-group
   [:select.form-control
    [:option "Correction One"]
    [:option "Correction Two"]]]])

(defn pagination-handler [e]
  "Update the pagination state's current page"
  (println "Pagination handler got" e)
  (swap! pager assoc :current-page e))

; (defn item-list
;   "Not curently used but shows a prettier view of the results
;   in list format with details rather than in a table."
;   []
;   [:div.list-group
;    (if (> (count @enrichment-results) 0)
;    (for [row (nth (partition 10 (sort-by :p-value < @enrichment-results)) (dec (:current-page @pager)))]
;      ^{:key (:identifier row)} [:div.list-group-item
;                                 (.log js/console "type is" (.toFixed (:p-value row) 2))
;                                 (let [x (:p-value row)]
;                                   (js/eval "debugger"))
;                                 [:div.row-action-primary [:i.material-icons (subs (str (:p-value row)) 1 3)]]
;                                 [:div.row-content
;                                  [:div.least-content (str (:matches row) " matches")]
;                                  [:h4.list-group-item-heading [:p.list-group-item-text (:description row)]]]]))])

(defn table
  "Table to display enrichment results"
  [rows-per-page enrichment-results]
  (fn [rows-per-page enrichment-results]
    [:div
    [:table
     [:thead
      [:th "Description"]
      [:th "p-value"]
      [:th "Matches"]]
     [:tbody
      (if (> (count enrichment-results) 0)
        (do
          (for [row (nth (partition
                          rows-per-page rows-per-page [nil] (sort-by :p-value < enrichment-results))
                         (dec (:current-page @pager)))]
            ^{:key (:identifier row)} [:tr
                                       [:td
                                        [:span (:description row)]
                                        [:a {:href
                                             (str "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids="
                                                  (:identifier row))}
                                         (str " [" (:identifier row) "]")]]
                                       ;  [:td (:identifier row)]
                                       [:td (:p-value row)]
                                       [:td (:matches row)]])))]]]))

(defn fetch-enrichment-chan [list-name enrichment-type]
  "Fetch templates from Intermine and return them over a channel"
  (let [enrichment-chan (chan)]
    (-> flymine (.enrichment (clj->js {:list list-name
                                       :widget enrichment-type
                                       :maxp 0.05
                                       :correction "Holm-Bonferroni"}))
        (.then (fn [results]
                 (go (>! enrichment-chan (js->clj results :keywordize-keys true))))))
    enrichment-chan))

(defn result-counter [vals]
  (fn [vals]
    [:h1 (str "contains " (count vals) " values")]))

(defn ^:export main [step-data]
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."

  (let [local-state (reagent/atom (merge {:current-page 1
                                      :rows-per-page 20}
                                     (last (:state step-data))))]
    (reagent/create-class
     {:reagent-render (fn [step-data]
                           [:div
                            [:h3 (:title (:state step-data))]
                            (if-not (nil? (:enrichment-results @local-state))

                              [paginator/main
                               {:rows (count (:enrichment-results @local-state))
                                :spread 5
                                :current-page (:current-page @local-state)
                                :rows-per-page (:rows-per-page @local-state)
                                :on-change pagination-handler}])
                            [table (:rows-per-page @local-state) (:enrichment-results @local-state)]])
      :component-will-receive-props (fn [this new-props]
                                      (let [props (extract-props new-props)
                                            enrichment-type (:widget (:state props))]
                                        (go
                                         (let [e (<! (fetch-enrichment-chan (:name (:data (:upstream-data props))) enrichment-type))]
                                           (println "E" (count e))
                                           (swap! local-state assoc :enrichment-results e)))))})))
