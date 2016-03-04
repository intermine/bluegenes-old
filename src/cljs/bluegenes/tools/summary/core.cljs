(ns bluegenes.tools.summary.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results "Fake Results. Don't believe in me"}))
(def local-state (reagent.core/atom nil))

(defn build-id-query [data]
  "Construct a query using an intermine list name."
  {:from "Gene"
   :select "*"
   :where [{:path "Gene.id"
            :values (:values data)
            :op "ONE OF"
            :code "A"}]})

(defn results-handler [results]
  (.log js/console "RESULTSSSS" (clj->js results))
  (reset! search-results
    {:results  (.-results results)}))

(defn get-data [data]
  "Resolves IDs via IMJS promise"
  (.log js/console "%cDaaaahta:" "color:cornflowerblue" (clj->js data))
  (let [service (:service data)
        q (build-id-query (:data data))]
          (go (let [response (<! (im/query {:service service} q))]
                (results-handler response)))))

(defn summary []
  [:div "Bob"
   @local-state
   [:div
    [:h5 "results"]
    @search-results]])

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
   {:reagent-render (fn []
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
