(ns bluegenes.tools.search.resultrow
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))
(enable-console-print!)

(defn is-selected? [result selected-result]
  (= selected-result result))

(defn result-selection-control [result state]
  [:input {:type "radio"
           :name "keyword-search" ;;todo, dynamic names. would we ever really have two keyword searches on one page though? That seems like madness!
           :checked (is-selected? result (:selected-result @state))
           }]
  )

(defn set-selected! [result state]
  (swap! state assoc :selected-result result)
  )


(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types and has a default that just outputs all non id, type, and relevance fields."
  (fn [result state] (.-type result)))

  (defmethod result-row "Gene" [result state]
    (let [details (.-fields result)]
      [:div {
        :on-click (fn [] (set-selected! result state))}
       [result-selection-control result state]
        [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
        [:span.organism (aget details "organism.name")]
        [:span " Symbol: " (.-symbol details) ]
        [:span.ids " Identifiers: " (.-primaryIdentifier details) ", " (.-secondaryIdentifier details)]
       ]))

 (defmethod result-row "Protein" [result state]
   (let [details (js->clj (.-fields result))]
     [:div {
       :on-click (fn [] (set-selected! result state))}
      [result-selection-control result state]
       [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
       [:span.organism (get details "organism.name")]
       [:span " Accession: " (get details "symbol" "unknown") ]
       [:span.ids " Identifiers: " (get details "primaryIdentifier" "unknown")]
      ]))


  (defmethod result-row "Publication" [result state]
    (let [details (.-fields result)]
      [:div {
        :on-click (fn [] (set-selected! result state))}
        [result-selection-control result state]
        [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
        [:span "Author: " (.-firstAuthor details)]
        [:cite " \"" (.-title details) "\""]
        [:span.journal " (" (.-journal details) " pp. " (.-pages details)] ")"
       ]))

(defmethod result-row "Author" [result state]
 [:div {
   :on-click (fn [] (set-selected! result state))}
  [result-selection-control result state]
  [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
    (aget result "fields" "name")])

(defmethod result-row :default [result state]
  "format a row in a readable way when no other templates apply. Adds 'name: description' default first rows if present."
  (let [details (js->clj (.-fields result))]
    [:div {
      :on-click (fn [] (set-selected! result state))}
     [result-selection-control result state]
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
