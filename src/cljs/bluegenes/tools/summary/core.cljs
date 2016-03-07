(ns bluegenes.tools.summary.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(def search-results (reagent.core/atom nil))
(def local-state (reagent.core/atom nil))

(defn build-id-query [data]
  "Construct a query using an intermine list name."
  {:from (:type data)
   :select "*"
   :where [{:path (str (:type data) ".id")
            :values (:values data)
            :op "ONE OF"
            :code "A"}]})

(defn results-handler [results]
  (.log js/console (clj->js results))
  (reset! search-results (first results)))

(defn get-data [data]
  "Resolves IDs via IMJS promise"
  (let [service (:service data)
        d (:data data)
        id (:payload d)
        type (:type d)]
          (go (let
            [response (<! (im/summary-fields {:service service} type id))]
              (results-handler response)))))

(defn is-good-result? [k v]
  (.log js/console "%c is good result? K:%s V:%s" "background:turquoise;" (clj->js k) v )
  (let [machine-fields #{:objectId}]
    (and (not (contains? machine-fields k))
    (some? v))
    ))

(defn summary []
  "Visual output of each of the summary fields returned."
   [:div.summary-fields
    [:h5 "Results"]
    (.log js/console @search-results)
    [:dl
    (for [[k v] @search-results]
      (if (is-good-result? k v)
      ^{:key k}
      [:div [:dt (clj->js k)] [:dd v]]))
    ]])

(defn ^:export preview
  "Render a preview of the tool."
  []
  (let [state (reagent/atom 0)]
    (fn [data]
      [:div
       [:div.heading "View Table"]
       [:div.indented (str @state " rows.")]]
      )))

(defn ^:export main
  "Render the main view of the tool."
  []
  (reagent/create-class
   {:display-name "Summary"
    :reagent-render (fn []
      [summary])
    :component-did-mount (fn [this]
        (let [passed-in-upstream (:upstream-data (reagent/props this))]
          (if (some? passed-in-upstream)
            (do (.log js/console "Passed in state" (clj->js passed-in-upstream))
              (reset! local-state passed-in-upstream)
              (get-data @local-state)))))
    :component-did-update (fn [new-stuff old-stuff]
      (let [passed-in-upstream (:upstream-data (reagent/props new-stuff))]
        (if (some? passed-in-upstream)
          (do (.log js/console "Passed in state" (clj->js passed-in-upstream))
            (reset! local-state passed-in-upstream)
            (get-data @local-state))))
      )}))
