(ns bluegenes.tools.search.resultrow
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))
(enable-console-print!)

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
