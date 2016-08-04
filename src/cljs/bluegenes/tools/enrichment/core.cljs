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
                   (println "EMITTING")
                   (has-something {:data     {:format  "query"
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
  (fn [step-data]
    (do
      [:div.table-wrapper
       [:table.table.table-striped.comp-table
        [table-header]
        [:tbody
         (if (> (count (-> step-data :cache :enrichment-results)) 0)
           (do
             (for [row (take 10 (-> step-data :cache :enrichment-results))]
               [:tr
                {:on-click (fn []
                             ((:has-something (:api step-data)) {:data     {:format  "query"
                                                             :type    (-> step-data :cache :path-constraint)
                                                             :payload (c/build-matches-query
                                                                        (-> step-data :cache :path-query)
                                                                        (-> step-data :cache :path-constraint)
                                                                        (:identifier row))}
                                                  :service  (-> step-data :input :service)
                                                  :shortcut "viewtable"}))}
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

(defn run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache] :as api}
   global-cache]

  (let [deconstructed (im/deconstruct-query-by-class (-> global-cache :models :flymine)
                                                     (-> snapshot :input :data :payload))]

    ; If we don't have cached ids, or the input has changed, get new ids
    (if (or (not (contains? cache :ids)) (contains? what-changed :input))
      (do
        ;(println "what-changed" what-changed)
        (let [query (-> deconstructed seq first second first :query)]
          (go (let [results (flatten (<! (im/query-rows {:service (:service (:input snapshot))} query)))]
                (save-cache (merge (:cache snapshot) {:ids results})))))))

    ; If our state has changed then merge it on top of the default values
    (if (contains? what-changed :state)
      (save-state (merge default-values (:state snapshot))))

    ; If our cache contains IDs we need to re-run:
    (if (contains? cache :ids)
      (let [parameters (merge default-values (:state snapshot) (select-keys cache [:ids]))]
        (go (let [res (<! (im/enrichment {:service (-> snapshot :input :service)} parameters))]
              (save-cache {:ids                    (:ids cache)
                           :enrichment-results     (-> res :results)
                           :path-query             (js->clj (.parse js/JSON (:pathQuery res)) :keywordize-keys true)
                           :path-query-for-matches (js->clj (.parse js/JSON (:pathQueryForMatches res)) :keywordize-keys true)
                           :path-constraint        (:pathConstraint res)})))))))

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (reagent/create-class
    {:reagent-render
     (fn [step-data]
       [:div.enrichment
        [:h3 (:title (:state step-data))]
        [:div (str "results: " (count (get-in step-data [:cache :enrichment-results])))]
        [enrichment-controls {:state         (:state step-data)
                              :replace-state (-> step-data :api :replace-state)}]
        [table step-data]])}))
