(ns bluegenes.tools.summary.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(defn get-id-query
  "Construct a query using an intermine list name."
  [list]
  {:from (:type list)
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"}]})

(defn summary [props]
  [:div "Bob"])

(defn ^:export preview
  "Render a preview of the tool."
  []
  (let [state (reagent/atom 0)]
    (fn [data]
      (update-count data state)
      [:div
       [:div.heading "View Table"]
       [:div.indented (str @state " rows.")]]
      )))

(defn ^:export main
  "Render the main view of the tool."
  []
  (reagent/create-class
   {:reagent-render (fn [props]
      [summary props])
      }))
