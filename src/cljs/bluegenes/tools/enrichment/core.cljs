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

(def pager (reagent/atom {:partition    10
                          :current-page 1}))

(defn call [f & args]
  (apply f args))

(defn enrichment-controls []
  (fn [{:keys [state replace-state]}]
    [:div
     [:div.row
      [:div.col-xs-6
       [:form
        [:span "Test Correction"]
        [:div.dropdown
         [:button.btn.btn-default.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          (str (:correction state))]
         [:ul.dropdown-menu
          [:li [:a {:on-click #(replace-state (merge state {:correction "Holms-Bonferroni"}))} "Holms-Bonferroni"]]
          [:li [:a {:on-click #(replace-state (merge state {:correction "Benjamini Hochberg"}))} "Benjamini Hochberg"]]
          [:li [:a {:on-click #(replace-state (merge state {:correction "Bonferroni"}))} "Bonferroni"]]
          [:li [:a {:on-click #(replace-state (merge state {:correction "None"}))} "None"]]]]]]
      [:div.col-xs-6
       [:form
        [:span "Maximum p-value:"]
        [:div.dropdown
         [:button.btn.btn-default.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          (str (:maxp state))]
         [:ul.dropdown-menu
          [:li [:a {:on-click #(replace-state (merge state {:maxp 0.05}))} "0.05"]]
          [:li [:a {:on-click #(replace-state (merge state {:maxp 0.10}))} "0.10"]]
          [:li [:a {:on-click #(replace-state (merge state {:maxp 1.00}))} "1.00"]]]]]]]

     [:div.row
      [:div.col-xs-12
       [:form.form-group
        [:span "Background"]
        [listdropdown/main {:on-change (fn [listname]
                                         (replace-state (merge state {:population listname})))
                            :title     "Change"
                            :service   {:root "www.flymine.org/query"}}]
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


(defn table-row [{:keys [row path-query path-query-for-matches path-constraint has-something service]}]
  (fn []
    [:tr
     [:td.description
      [:span (:description row)]
      [:span (str " [" (:identifier row) "]")]]
     [:td (.. (:p-value row) (toPrecision 6))]
     [:td
      {:on-click (fn []
                   (has-something {:data
                                             {:format  "query"
                                              :type    path-constraint
                                              :payload (c/build-matches-query
                                                         path-query
                                                         path-constraint
                                                         (:identifier row))}
                                   :service  (:service service)
                                   :shortcut "viewtable"}))}
      [:div.btn.btn-raised.btn-info (:matches row)]]]))

(defn table
  "Table to display enrichment results"
  []
  (fn [enrichment-results]
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

(def default-values {:current-page  1
                     :rows-per-page 20
                     :widget        "enrichment-type"
                     :title         "Generic Displayer"
                     :maxp          0.05
                     :format        "json"
                     :population    nil
                     :correction    "Bonferroni"})

(defn handle-update
  "Fetch enrichment results from Intermine and save the results."
  [component]
  (let [{:keys [api upstream-data state]} (reagent/props component)]
    ; Start with default values for enrichment, then merge in the state
    (let [replace-state         (:replace-state api)
          enrichment-parameters (merge default-values
                                       state
                                       (cond
                                         (= "list" (:format (:data upstream-data)))
                                         {:list (:payload (:data upstream-data))}
                                         (= "ids" (:format (:data upstream-data)))
                                         {:ids (:payload (:data upstream-data))}))]
      (go (let [res (<! (im/enrichment (select-keys upstream-data [:service]) enrichment-parameters))]
            (replace-state (update-in enrichment-parameters [:cache]
                                      assoc
                                      :enrichment-results (-> res :results)
                                      :path-query (js->clj (.parse js/JSON (:pathQuery res)) :keywordize-keys true)
                                      :path-query-for-matches (js->clj (.parse js/JSON (:pathQueryForMatches res)) :keywordize-keys true)
                                      :path-constraint (:pathConstraint res))))))))

(defn run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache] :as api}
   global-cache]

  (let [replace-state         (:save-state api)
        deconstructed         (im/deconstruct-query-by-class (-> global-cache :models :flymine)
                                                             (-> input :data :payload))
        ;enrichment-parameters (merge default-values state (cond
        ;                                                    ;(= "list" (:format (:data input)))
        ;                                                    ;{:list (:payload (:data input))}
        ;                                                    ;(= "query" (:format (:data input)))
        ;                                                    (= "query" "one")
        ;                                                    {:ids (-> input :data :payload)}))
        ]

    ;(case (count deconstructed)
    ;  1 (println "HAS JUST ONE" (-> deconstructed seq first second))
    ;  2 (println "HAS TWO" (keys deconstructed)))

    ;(println "cachen" cache)

    (if (or (not (contains? cache :ids)) (contains? what-changed :input))
      (let [query (-> deconstructed seq first second first :query)]
        (go (let [results (flatten (<! (im/query-rows {:service (:service input)} query)))]
              (save-cache {:ids results})))))

    (if (contains? cache :ids)
      (let [parameters (merge default-values (:state snapshot) (select-keys cache [:ids]))]
        (println "doing parametesr" (-> snapshot :input :service))
        (go (let [results (<! (im/enrichment {:service (-> snapshot :input :service)} parameters))]
              (println "TRUE RESULTS" results)))
        ))



    ;(println "deconstructed" (count (keys deconstructed)))
    ;(println "enrichment params" enrichment-parameters)
    ;(go (let [res (<! (im/enrichment (select-keys upstream-data [:service]) enrichment-parameters))]
    ;      (replace-state (update-in enrichment-parameters [:cache]
    ;                                assoc
    ;                                :enrichment-results (-> res :results)
    ;                                :path-query (js->clj (.parse js/JSON (:pathQuery res)) :keywordize-keys true)
    ;                                :path-query-for-matches (js->clj (.parse js/JSON (:pathQueryForMatches res)) :keywordize-keys true)
    ;                                :path-constraint (:pathConstraint res)))))
    )
  )

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (reagent/create-class
    {:reagent-render
     (fn [step-data]
       [:div.enrichment
        [:h3 (:title (:state step-data))]
        [:div (str "results: " (count (get-in step-data [:state :cache :enrichment-results])))]
        [enrichment-controls {:state         (:state step-data)
                              :replace-state (-> step-data :api :replace-state)}]
        [table (-> step-data :state :cache :enrichment-results)]])
     ;:component-did-mount handle-update
     ;:component-did-update handle-update
     }))
