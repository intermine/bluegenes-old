(ns bluegenes.components.lighttable.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [intermine.imtables :as imtables]
            [cljs-http.client :as http]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]
            [cljs.core.async :as a :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(defn get-list-query
  "Construct a query using a collection of object ids."
  [payload]
  {:from   (:type payload)
   :select "*"
   :where  [{:path  (:type payload)
             :op    "IN"
             :value (:payload payload)
             :code  "A"}]})

(defn get-id-query
  "Construct a query using an intermine list name."
  [payload]
  {:from   (:type payload)
   :select "*"
   :where  [{:path   (str (:type payload) ".id")
             :values (:payload payload)
             :op     "ONE OF"
             :code   "A"}]})

(defn normalize-input
  "Convert a variety of inputs into an imjs compatible clojure map."
  [input-data]
  (cond
    (= "list" (-> input-data :data :format))
    (get-list-query (get-in input-data [:data]))
    (= "ids" (-> input-data :data :format))
    (get-id-query (get-in input-data [:data]))
    (= "query" (-> input-data :data :format))
    (get-in input-data [:data :payload])))

(defn table-cell []
  (fn [d]
    [:td d]))

(defn table-row []
  (fn [row-data]
    (into [:tr] (map (fn [x] [table-cell x]) row-data))))

(defn table
  [xatom]
  (fn []
    [:table.table.table-striped
     [:thead
      (into [:tr] (map (fn [x] [:th x]) (:columnHeaders @xatom)))]
     [:tbody (map (fn [x] [table-row x]) (:results @xatom))]]))

(defn swap-query-results
  [component a cache]
  (let [props (reagent/props component)]
    (go (let [r (<! (im/raw-query-rows {:root "www.flymine.org/query"}
                                       (normalize-input props)
                                       {:size   10
                                        :format "json"}))]
          (reset! a r)))))

(defn swap-query-summary
  [component a cache]
  (let [props (reagent/props component)]
    (go (let [r (<! (im/summarize-query (-> cache :models :flymine)
                                        {:root "www.flymine.org/query"}
                                        (normalize-input props)))]
          (reset! a r)))))

(defn countbox
  [atom]
  (fn []
    [:div (str @atom)]
    ;[:div (str (map (fn [[path details]] [path (:count details)] ) @atom))]
    ))

(defn county []
  (let [xatom        (reagent/atom nil)
        global-cache (re-frame/subscribe [:global-cache])]
    (reagent/create-class
      {:component-did-mount  #(swap-query-summary % xatom @global-cache)
       :component-did-update #(swap-query-summary % xatom @global-cache)
       :reagent-render       (fn [] [countbox xatom])})))


(defn mounty []
  (let [xatom        (reagent/atom nil)
        global-cache (re-frame/subscribe [:global-cache])]
    (reagent/create-class
      {:component-did-mount  #(swap-query-results % xatom @global-cache)
       :component-did-update #(swap-query-results % xatom @global-cache)
       :reagent-render       (fn [] [table xatom])})))

(defn ^:export main []
  (fn [step-data]
    [:div.lighttable-container
     [:h3 "Results"]
     [county step-data]
     [mounty step-data]]))

