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

(defn results-handler [results service type]
  "First, apply the results to the page, then go fetch the names of the fields and replace the unpleasant looking javascript machine names that were there."
  (reset! search-results results)
  (doall
    (for [[k v] results]
      (cond (im/is-good-result? k v)
        ;;TODO: cache this stuff. it doesn't change and doesn't need to be downloaded repeatedly
        (go (let [display-name (<! (im/get-display-name service type k))]
          (swap! search-results assoc-in [k :name] display-name)))))))


(defn get-data [data]
  "Resolves IDs via IMJS promise"
  (let [service (:service data)
        d (:data data)
        id (:payload d)
        type (:type d)]
          (go (let
            [response (<! (im/summary-fields {:service service} type id))]
              (results-handler response service type)))))

(defn summary-row [val]
  "casts :val val to string, to ensure boolean values output on the screen.
  previously they disappeared like magic."
  [:div
   [:dt (clj->js (:name val))] [:dd (str (:val val))]])

(defn summary-title []
  "outputs the result name if present, or just 'summary' otherwise"
  [:h5
   (let [name (:val (:name @search-results))]
    (cond name (str name " ")))
    "Summary"])

(defn summary []
  "Visual output of each of the summary fields returned. Has default locations at top and bottom for more/less significant fields."
   [:div.summary-fields
    [summary-title]
    [:dl
     ;;output certain fields in preferred locations, if they exist.
     ;;these fields go at the start (Rachel says)
      (cond (:name @search-results) [summary-row (:name @search-results)])
      (cond (:title @search-results) [summary-row (:title @search-results)])
     ;;output everything else
      (for [[k v] (dissoc @search-results :abstractText :description :name :title)]
        (if (im/is-good-result? k v)
          ^{:key (:name v)}
          [summary-row v]))
     ;;these need to go at the end (Rachel says).
      (cond (:description @search-results) [summary-row (:description @search-results)])
      (cond (:abstractText @search-results) [summary-row (:abstractText @search-results)])
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
            (do
              (reset! local-state passed-in-upstream)
              (get-data @local-state)))))
    :component-did-update (fn [new-stuff old-stuff]
      (let [passed-in-upstream (:upstream-data (reagent/props new-stuff))]
        (if (some? passed-in-upstream)
          (do
            (reset! local-state passed-in-upstream)
            (get-data @local-state)))))
    :component-will-update (fn []
      (reset! search-results nil))
    }))
