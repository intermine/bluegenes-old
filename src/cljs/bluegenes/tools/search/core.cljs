(ns bluegenes.tools.search.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]
            [bluegenes.tools.search.filters :as filters]
            [bluegenes.tools.search.resultrow :as resulthandler]            [json-html.core :as json-html])

    (:use [json-html.core :only [edn->hiccup]]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))
(def results-counter (reagent.core/atom {:shown-results-count 0}))
(def max-results 99);;todo - this is only used in a cond right now, won't modify number of results returned. IMJS was being tricky;

(defn sort-by-value [result-map]
  "Sort map results by their values. Used to order the category maps correctly"
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get result-map key2) key2]
                                  [(get result-map key1) key1])))
        result-map))

(defn results-handler [results mine api searchterm]
  "Store results in local state once the promise comes back."
  (if (:active-filter @search-results)
    ;;if we're resturning a filter result, leave the old facets intact.
    (swap! search-results
      assoc :results (.-results results))
    ;;if we're returning a non-filtered result, add new facets to the atom
    (reset! search-results
      {
      :results  (.-results results)
      :facets {
        :organisms (sort-by-value (js->clj (aget results "facets" "organism.shortName")))
        :category (sort-by-value (js->clj (aget results "facets" "Category")))}}))
  ((:append-state api) {:input searchterm :results @search-results}))

(defn search
  "search for the given term via IMJS promise. Filter is optional"
  [searchterm api & filter]
    (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
          search {:q searchterm :Category filter}
          id-promise (-> mine (.search (clj->js search)))]
      (-> id-promise (.then
          (fn [results]
            (results-handler results mine api searchterm))))))

(defn submit-handler [searchterm api]
  "Adds search term to the state, and searches for the term"
  (search searchterm api))

(defn is-active-result? [result]
  "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
    (or
      (= (:active-filter @search-results) (.-type result))
      (nil? (:active-filter @search-results))))

(defn count-total-results [state]
  "returns total number of results by summing the number of results per category. This includes any results on the server beyond the number that were returned"
  (reduce + (vals (:category (:facets state))))
  )

(defn count-current-results []
  "returns number of results currently shown, taking into account result limits nd filters"
  (count
    (remove
      (fn [result]
        (not (is-active-result? result))) (:results @search-results))))


(defn results-count []
  "Visual component: outputs the number of results shown."
    [:small " Displaying " (count-current-results) " of " (count-total-results @search-results) " results"])

(defn load-more-results [api search-term]
  (.log js/console (clj->js @search-term))
  (search @search-term api (:active-filter @search-results))
  )

(defn results-display [api search-term]
  "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
  [:div.results
    [:h4 "Results" [results-count]]
    [:form
      (doall (let [state search-results
        ;;active-results might seem redundant, but it outputs the results we have client side
        ;;while the remote results are loading. Good for slow connections.
        active-results (filter (fn [result] (is-active-result? result)) (:results @state))
        filtered-result-count (get (:category (:facets @state)) (:active-filter @state))]
          ;;load more results if there are less than our preferred number, but more than
          ;;the original search returned
          (cond (and  (< (count-current-results) filtered-result-count)
                      (<= (count-current-results) max-results))
            (load-more-results api search-term))
          ;;output em!
          (for [result active-results]
            ^{:key (.-id result)}
            [resulthandler/result-row {:result result :state state :api api}])))]
   ])

(defn check-for-query-string-in-url []
  "Splits out the search term from the URL, allowing repeatable external linking to searches"
  (let [url (aget js/window "location" "href")
        last-section (str/split url #"/search\?")]
    (if (> (count last-section) 1) ;; if there's a query param, eg "someurl.com/#/timeline/search?fkh"
      (last last-section)
      nil)))

(defn search-form [search-term api]
  "Visual form component which handles submit and change"
  [:div.search
    [:form.searchform {:on-submit (fn [e]
      (.preventDefault js/e)
        (submit-handler @search-term api))}
        [:input {
          :type "text"
          :placeholder "Search for a gene, protein, disease, etc..."
          :value @search-term
          :on-change (fn [val]
              (reset! search-term (-> val .-target .-value)))}]
    [:button "Submit"]]
   [:div.response
      [filters/facet-display search-results api search @search-term]
      [results-display api search-term]]])

(defn ^:export main []
  (let [search-term (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [search-form search-term api])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))
              query-string (check-for-query-string-in-url)
              api (:api (reagent/props this))]
          (reset! search-term (:input passed-in-state))
          (swap! search-results assoc :query-string query-string)
          ;populate the form from the url if there's a query param
          (cond (some? query-string)
            (do
              (reset! search-term query-string)
              (submit-handler query-string api)
              ))))
})))
