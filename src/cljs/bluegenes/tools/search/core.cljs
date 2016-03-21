(ns bluegenes.tools.search.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]
            [bluegenes.tools.search.filters :as filters]
            [bluegenes.tools.search.resultrow :as resulthandler]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))
(def results-counter (reagent.core/atom {:shown-results-count 0}))

(defn sort-by-value [result-map]
  "Sort map results by their values. Used to order the category maps correctly"
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get result-map key2) key2]
                                  [(get result-map key1) key1])))
        result-map))

(defn results-handler [results mine api]
  "Store results in local state once the promise comes back."
  (reset! search-results
    {
    :results  (.-results results)
    :facets {
      :organisms (sort-by-value (js->clj (aget results "facets" "organism.shortName")))
      :category (sort-by-value (js->clj (aget results "facets" "Category")))}})
  )


(defn submit-handler [searchterm api]
  "Resolves IDs via IMJS promise"
  ((:append-state api) {:input searchterm})
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        search {:q searchterm}
        id-promise (-> mine (.search (clj->js search)))]
    (-> id-promise (.then
        (fn [results]
          (results-handler results mine api))))))

(defn is-active-result? [state result]
  "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
    (or
      (= (:active-filter @state) (.-type result))
      (nil? (:active-filter @state))))

(defn count-total-results [state]
  "returns total number of results by summing the number of results per category. This includes any results on the server beyond the number that were returned"
  (reduce + (vals (:category (:facets state))))
  )

(defn count-current-results [state]
  "returns number of results currently shown, taking into account result limits nd filters"
  (count
    (remove
      (fn [result]
        (not (is-active-result? state result))) (:results @state))))


(defn results-count [state]
  "Visual component: outputs the number of results shown."
    [:small " Displaying " (count-current-results state) " of " (count-total-results @state) " results"])

(defn results-display [state api]
  "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
  [:div.results
    [:h4 "Results" [results-count state]]
    [:form
     (let [active-results (filter (fn [result] (is-active-result? state result)) (:results @state))]
     (doall (for [result (:results @state)]
         ^{:key (.-id result)}
         [resulthandler/result-row {:result result :state state :api api}]
         )))]])

(defn check-for-search-term-in-url []
  "Splits out the search term from the URL, allowing repeatable external linking to searches"
  (let [url (aget js/window "location" "href")
        last-section (str/split url #"/search\?")]
    (if (> (count last-section) 1) ;; if there's a query param, eg "someurl.com/#/timeline/search?fkh"
      (last last-section)
      nil)))

(defn search-form [local-state api]
  "Visual form component which handles submit and change"
  [:div.search
  [:form.searchform {:on-submit (fn [e]
      (.preventDefault js/e)
      (let [searchterm @local-state]
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
      [results-display search-results api]]])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [search-form local-state api])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))
              search-term (check-for-search-term-in-url)
              api (:api (reagent/props this))]
          (reset! local-state (:input passed-in-state))
          ;populate the form from the url if there's a query param
          (cond (some? search-term)
            (do
              (reset! local-state search-term)
              (submit-handler search-term api)
              ))))
      :component-did-update (fn [this old-props]
        (.log js/console "did update" this old-props))})))
