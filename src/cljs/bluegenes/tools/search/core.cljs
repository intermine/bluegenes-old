(ns bluegenes.tools.search.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]
            [bluegenes.tools.search.filters :as filters]
            [bluegenes.tools.search.resultrow :as resulthandler]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))

(defn sort-by-value [result-map]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get result-map key2) key2]
                                  [(get result-map key1) key1])))
        result-map))

(defn results-handler [results mine comm]
  "Emit our results once the promise comes back."
  (.log js/console "%cresults" "background-color:ivory" results)
  (reset! search-results
    {
    :results  (.-results results)
    :facets {
      :organisms (sort-by-value (js->clj (aget results "facets" "organism.shortName")))
      :category (sort-by-value (js->clj (aget results "facets" "Category")))}})
  )


(defn submit-handler [searchterm comm]
  "Resolves IDs via IMJS promise"
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        search {:q searchterm}
        id-promise (-> mine (.search (clj->js search)))]
    (-> id-promise (.then
        (fn [results]
          (results-handler results mine comm))))))

(defn is-active-result? [state result]
  "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
    (or
      (= (:active-filter state) (.-type result))
      (nil? (:active-filter state))))

(defn results-display [state]
  "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
  [:div.results
    [:h4 "Results"]
   (for [result (:results state)]
     (if (is-active-result? state result)
     ^{:key (.-id result)}
     [resulthandler/result-row result]))
   ])


(defn id-form [local-state api]
  "Visual form component which handles submit and change"
  [:div.search
  [:form {:on-submit (fn [e]
      (.preventDefault js/e)
      (let [searchterm @local-state]
        ((:append-state api) {:input searchterm})
        (submit-handler searchterm api)))}
        [:input {
          :type "text"
          :placeholder "Search for a gene, protein, disease, etc..."
          :value @local-state
          :on-change (fn [val]
              (reset! local-state (-> val .-target .-value)))}]
    [:button "Submit"]]
   [:div.response
      [filters/facet-display search-results]
      [results-display @search-results]]])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [id-form local-state api])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))]
          (reset! local-state (:input passed-in-state))))
      :component-did-update (fn [this old-props]
        (.log js/console "did update" this old-props))})))
