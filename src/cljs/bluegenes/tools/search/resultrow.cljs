(ns bluegenes.tools.search.resultrow
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))
(enable-console-print!)

(defn is-selected? [result selected-result]
  "returns true if 'result' is selected"
  (= selected-result result))

(defn result-selection-control [result state]
  "UI control suggesting to the user that there is only one result selectable at any one time; there's no actual form functionality here."
  [ :input {
      :type "radio"
      :name "keyword-search" ;;todo, dynamic names. would we ever really have two keyword searches on one page though? That seems like madness!
      :checked (is-selected? result (:selected-result @state))}])

(defn set-selected! [row-data]
  "sets the selected result in the local state atom and emits that we 'have' this item to next steps / the next tool"
  (swap! (:state row-data) assoc :selected-result (:result row-data))
  ;;Todo: remove this dirty hard coding of the service URL
  ((:has-something (:api row-data))
   {:service {:root "www.flymine.org/query"}
       :data {
          :format "ids"
          :payload [(.-id (:result row-data))]
          :type (.-type (:result row-data))}}))

(defn row-structure [row-data contents]
  "This method abstracts away most of the common components for all the result-row baby methods."
  (let [result (:result row-data) state (:state row-data)]
  [:div.result {
    :on-click (fn [] (set-selected! row-data))
    :class (if (is-selected? result (:selected-result @state)) "selected")}
    [result-selection-control result state]
    [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
    (contents)]
  ))

(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types and has a default that just outputs all non id, type, and relevance fields."
  (fn [row-data] (.-type (:result row-data))))

(defmethod result-row "Gene" [row-data]
  (let [details (.-fields (:result row-data))]
    [row-structure row-data (fn []
      [:div.details
        [:span.organism (aget details "organism.name")]
        [:span " Symbol: " (.-symbol details) ]
        [:span.ids " Identifiers: " (.-primaryIdentifier details) ", " (.-secondaryIdentifier details)]])]))

(defmethod result-row "Protein" [row-data]
 (let [details (js->clj (.-fields (:result row-data)))]
   [row-structure row-data (fn []
     [:div.details
        [:span.organism (get details "organism.name")]
        [:span " Accession: " (get details "symbol" "unknown") ]
        [:span.ids " Identifiers: " (get details "primaryIdentifier" "unknown")]])]))


(defmethod result-row "Publication" [row-data]
  (let [details (.-fields (:result row-data))]
  [row-structure row-data (fn []
    [:div.details
      [:span "Author: " (.-firstAuthor details)]
      [:cite " \"" (.-title details) "\""]
      [:span.journal " (" (.-journal details) " pp. " (.-pages details)] ")"])]))

(defmethod result-row "Author" [row-data]
  [row-structure row-data (fn []
    [:div.details
      (aget (:result row-data) "fields" "name")])])

(defmethod result-row :default [row-data]
  "format a row in a readable way when no other templates apply. Adds 'name: description' default first rows if present."
  (let [details (js->clj (.-fields (:result row-data)))]
  [row-structure row-data (fn []
    [:div.details
    (if (contains? details "name")
      [:span.name (get details "name")])
    (if (contains? details "description")
      [:span.description (get details "description")])
     (for [[k value] details]
       (if (and (not= k "name") (not= k "description"))
       ^{:key k}
       [:span [:span.default-description k] [:span.default-value value]]))
  ])]))
