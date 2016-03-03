(ns bluegenes.tools.enrichment.core
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.components.paginator :as paginator]
            [bluegenes.tools.enrichment.controller :as c]
            [bluegenes.utils.imcljs :as im]
            [reagent.impl.util :as impl :refer [extract-props]]))

(def pager (reagent/atom {:partition 10
                          :current-page 1}))

(defn call [f & args]
  (apply f args))

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

(defn table-header []
  [:thead
   [:th "Description"]
   [:th "p-value"]
   [:th "Matches"]])

(defn pagination-handler [e]
  "Update the pagination state's current page"
  (println "Pagination handler got" e)
  (swap! pager assoc :current-page e))


(defn table-row [row path-query-for-matches path-constraint has-something]
  [:tr
   [:td
    [:span (:description row)]
    [:span (str " [" (:identifier row) "]")]]
   [:td (.. (:p-value row) (toPrecision 6) )]
   [:td
    {:on-click (fn []
                 (has-something {:data
                                 {:format "query"
                                  :type path-constraint
                                  :payload (c/build-matches-query
                                          path-query-for-matches
                                          path-constraint
                                          (:identifier row))}
                                 :service {:root "http://www.flymine.org/query"}
                                 :shortcut "viewtable"}))}
    [:div.btn.btn-raised.btn-info (:matches row)]]])

(defn table
  "Table to display enrichment results"
  []
  (fn [{:keys [rows-per-page
               enrichment-results
               path-query-for-matches
               path-constraint]}
       {:keys [has-something]}]
    (if (empty? enrichment-results)
      (do
        [:p "No results"])
      (do
        [:div.table-wrapper
         [:table.table.table-striped.comp-table
          [table-header]
          [:tbody
           (if (> (count enrichment-results) 0)
             (do
               (for [row (take 10 enrichment-results)]
                 (if-not (nil? row)
                   ^{:key (:identifier row)} [table-row
                                              row path-query-for-matches
                                              path-constraint
                                              has-something]))))]]]))))


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
         [table @local-state (:api step-data)]])
      :component-will-receive-props
      (fn [this new-props]
        (let [{:keys [upstream-data api state]} (extract-props new-props)
              enrichment-type (:widget state)]
          (call (:is-loading api) true)
          (swap! local-state assoc :enrichment-results nil)
          (go (let [res (<! (im/enrichment
                             (select-keys upstream-data [:service])
                             {:list (:payload (:data upstream-data))
                              :widget enrichment-type
                              :maxp 0.05
                              :format "json"
                              :correction "Holm-Bonferroni"}))]
                (call (:is-loading api) false)
             (swap! local-state assoc
                    :path-query-for-matches (js->clj (.parse js/JSON (:pathQueryForMatches res)) :keywordize-keys true)
                    :path-constraint (:pathConstraint res)
                    :enrichment-results (-> res :results))))))})))
