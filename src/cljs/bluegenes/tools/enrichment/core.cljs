(ns bluegenes.tools.enrichment.core
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.components.paginator :as paginator]
            [bluegenes.components.listdropdown :as listdropdown]
            [bluegenes.tools.enrichment.controller :as c]
            [bluegenes.utils.imcljs :as im]
            [reagent.impl.util :as impl :refer [extract-props]]))

(def pager (reagent/atom {:partition 10
                          :current-page 1}))

(defn call [f & args]
  (apply f args))

(defn enrichment-controls [api-fn state]
  (fn []
    [:div
    [:div.row
     [:div.col-xs-6
      [:form
       [:span "Test Correction"]
       [:select.form-control
        {:value (:correction @state)
         :on-change (fn [e]
                      (api-fn (merge @state {:correction (.. e -target -value) })))}
        [:option "Holms-Bonferroni"]
        [:option "Benjamini Hochberg"]
        [:option "Bonferroni"]
        [:option "None"]]]]
     [:div.col-xs-6
      [:form
       [:span "Maximum p-value:"]
       [:select.form-control
       {:value (:maxp @state)
        :on-change (fn [e]
                     (api-fn (merge @state {:maxp (.. e -target -value) })))}
        [:option "0.05"]
        [:option "0.10"]
        [:option "1.0"]]]]]

    [:div.row
     [:div.col-xs-12
      [:form.form-group
       [:span "Background"]
       [listdropdown/main {:on-change (fn [listname]
                                        (api-fn (merge @state {:population listname})))
                           :title "Change"
                           :service {:root "www.flymine.org/query"}}]
      ;  [:input.form-control {:type "text"}]
      ;  [listdropdown/main]
       ]]]]))


(defn table-header []
  [:thead
   [:th "Description"]
   [:th "p-value"]
   [:th "Matches"]])

(defn pagination-handler [e]
  "Update the pagination state's current page"
  (swap! pager assoc :current-page e))


(defn table-row [row path-query path-query-for-matches path-constraint has-something service]
  (fn []
    [:tr
     [:td.description
      [:span (:description row)]
      [:span (str " [" (:identifier row) "]")]]
     [:td (.. (:p-value row) (toPrecision 6) )]
     [:td
      {:on-click (fn []
                   (has-something {:data
                                   {:format "query"
                                    :type path-constraint
                                    :payload (c/build-matches-query
                                            path-query
                                            path-constraint
                                            (:identifier row))}
                                   :service (:service service)
                                   :shortcut "viewtable"}))}
      [:div.btn.btn-raised.btn-info (:matches row)]]]))

(defn table
  "Table to display enrichment results"
  []
  (fn [{:keys [enrichment-results] :as x}]
    (do
      [:div.table-wrapper
       [:table.table.table-striped.comp-table
        [table-header]
        [:tbody
         (if (> (count enrichment-results) 0)
           (do
             (for [row (take 10 enrichment-results)]
               [:tr
                [:td (:description row)]
                [:td (:p-value row)]
                [:td (:matches row)]])))]]])))

(def default-values {:current-page 1
                     :rows-per-page 20
                     :widget "enrichment-type"
                     :title "Generic Displayer"
                     :maxp 0.05
                     :format "json"
                     :population nil
                     :correction "Bonferroni"})

(defn handle-update [data]
  (let [{:keys [api upstream-data state]} (reagent/props data)]
    (go (let [res (<! (im/enrichment
                        (select-keys upstream-data [:service])
                        (merge default-values
                               state
                               (cond
                                 (= "list" (:format (:data upstream-data)))
                                 {:list (:payload (:data upstream-data))}
                                 (= "ids" (:format (:data upstream-data)))
                                 {:ids (:payload (:data upstream-data))}))))]
          ((:replace-state api) (assoc (merge default-values state) :enrichment-results (-> res :results)))))))

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (fn [step-data]
    (reagent/create-class
     {:reagent-render
      (fn [step-data]
        [:div.enrichment
         [:h3 (:title (:state step-data))]
         ;[enrichment-controls (-> step-data :api :replace-state)]
         [table (:state step-data)]])
      :component-did-mount handle-update
      :component-did-update handle-update})))


;(swap! local-state assoc
;       :path-query (js->clj (.parse js/JSON (:pathQuery res)) :keywordize-keys true)
;       :path-query-for-matches (js->clj (.parse js/JSON (:pathQueryForMatches res)) :keywordize-keys true)
;       :path-constraint (:pathConstraint res)
;       :enrichment-results (-> res :results))
