(ns bluegenes.tools.search.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))


(defn results-handler [results mine comm]
  "Emit our results once the promise comes back."
  (.log js/console "%cresults" "background-color:ivory" results)
  (reset! search-results
    {
    :results  (.-results results)
    :facets {
      :organisms (js->clj (aget results "facets" "organism.shortName"))
      :category (js->clj (aget results "facets" "Category"))}})
  )

(defn submit-handler [searchterm comm]
  "Resolves IDs via IMJS promise"
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        search {:q searchterm}
        id-promise (-> mine (.search (clj->js search)))]
    (-> id-promise (.then
        (fn [results]
          (results-handler results mine comm))))))

(defn facet-display [facets]
  [:div.facets
    [:h4 "Filter by:"]
      [:div
       [:h5 "Organisms"]
       [:ul
        (for [[name value] (:organisms facets)]
          ^{:key name}
          [:li
           [:span.count value] name])]
       [:h5 "Categories"]
       [:ul
      (for [[name value] (:category facets)]
        ^{:key name}
        [:li
         [:span.count.result-type {:class (str "type-" name)} value] name])]
       ]])

(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types and has a default that just outputs all non id, type, and relevance fields."
  (fn [result] (.-type result)))

  (defmethod result-row "Gene" [result]
    (let [details (.-fields result)]
      [:div
        [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
        [:span.organism (aget details "organism.name")]
        [:span " Symbol: " (.-symbol details) ]
        [:span.ids " Identifiers: " (.-primaryIdentifier details) ", " (.-secondaryIdentifier details)]
       ]))

 (defmethod result-row "Protein" [result]
   (let [details (js->clj (.-fields result))]
     [:div
       [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
       [:span.organism (get details "organism.name")]
       [:span " Accession: " (get details "symbol" "unknown") ]
       [:span.ids " Identifiers: " (get details "primaryIdentifier" "unknown")]
      ]))


  (defmethod result-row "Publication" [result]
    (let [details (.-fields result)]
      [:div
        [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
        [:span "Author: " (.-firstAuthor details)]
        [:cite " \"" (.-title details) "\""]
        [:span.journal " (" (.-journal details) " pp. " (.-pages details)] ")"
       ]))

(defmethod result-row "Author" [result]
 [:div
  [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
    (aget result "fields" "name")])

(defmethod result-row :default [result]
  "format a row in a readable way when no other templates apply. Adds 'name: description' default first rows if present."
  (let [details (js->clj (.-fields result))]
    [:div
    [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
    (if (contains? details "name")
      [:span.name (get details "name")])
    (if (contains? details "description")
      [:span.description (get details "description")])
     (for [[k value] details]
       (if (and (not= k "name") (not= k "description"))
       ^{:key k}
       [:span [:span.default-description k] [:span.default-value value]]))
  ]))


(defn results-display [resultlist]
  "Iterate through results and output one row per result using result-row to format"
  [:div.results
    [:h4 "Results"]
   (for [result (:results resultlist)]
     ^{:key (.-id result)}
     [result-row result])
   [:div]])


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
   [:div.form-group
    [:button "Submit"]]]
   [:div.response
      [facet-display (:facets @search-results)]
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
