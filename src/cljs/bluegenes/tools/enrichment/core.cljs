(ns bluegenes.tools.enrichment.core
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]
            [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.components.paginator :as paginator]
            [bluegenes.utils.imcljs :as im]
            [reagent.impl.util :as impl :refer [extract-props]]))

(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(def pager (reagent/atom {:partition 10
                  :current-page 1}))

(defn enrichment-controls []
  [:div.row
   [:div.col-xs-4

    [:form.form-group
     [:span "Test Correction"]
     [:select.form-control
      [:option "Holms-Bonferroni"]
      [:option "Benjamini Hochberg"]
      [:option "Bonferroni"]
      [:option "None"]]]]
   [:div.col-xs-4
    [:form.form-group
     [:span "Maximum p-value:"]
     [:select.form-control
      [:option "0.05"]
      [:option "0.10"]
      [:option "1.0"]]]]

   [:div.col-xs-4
    [:form.form-group
     [:span "Background"]
     [:input.form-control {:type "text"}]]]])

(defn pagination-handler [e]
  "Update the pagination state's current page"
  (println "Pagination handler got" e)
  (swap! pager assoc :current-page e))



(defn table
  "Table to display enrichment results"
  [rows-per-page enrichment-results]
  (fn [rows-per-page enrichment-results]
    ; (println "ENRICHMENT RESULTS" enrichment-results)
    (if (empty? enrichment-results)
      (do
        [:p "No results"])
      (do
        [:div.table-wrapper
         [:table.table.table-striped.comp-table
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
                 (if-not (nil? row)
                   ^{:key (:identifier row)} [:tr
                                              [:td
                                               [:span (:description row)]
                                               [:a {:href
                                                    (str "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids="
                                                         (:identifier row))}
                                                (str " [" (:identifier row) "]")]]
                                              ;  [:td (:identifier row)]
                                              ; (println "type" (.. (:p-value row) (toPrecision 6) ) )
                                              [:td (.. (:p-value row) (toPrecision 6) )]
                                              [:td (:matches row)]]))))]]]))))


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
     {:reagent-render
      (fn [step-data]
        [:div
         [:h3 (:title (:state step-data))]
         [enrichment-controls]
         [table (:rows-per-page @local-state) (:enrichment-results @local-state)]])
      :component-will-receive-props
      (fn [this new-props]

        (let [props (extract-props new-props)
              enrichment-type (:widget (:state props))]
          ((:is-loading (:api props)) true)
          (swap! local-state assoc :enrichment-results nil)
          (go
           (let [res (<! (im/enrichment
                          {:service {:root "http://www.flymine.org/query/"}}
                          {:list (:name (:data (:upstream-data props)))
                           :widget enrichment-type
                           :maxp 0.05
                           :format "json"
                           :correction "Holm-Bonferroni"}))]
             ((:is-loading (:api props)) false)
             (swap! local-state assoc :enrichment-results (-> res :results))))))})))
