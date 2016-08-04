(ns bluegenes.components.imtable.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [intermine.imtables :as imtables]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

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

(defn somefn [component]
  (let [{:keys [service data] :as dd} (reagent/props component)
        node   (reagent/dom-node component)
        target (.item (.getElementsByClassName node "imtable") 0)]


    (println "service daa" service data)
    (println "normalized" (normalize-input dd))

    (println "CAN RUN" (every? false? (map nil? [service data])))
    (println "running with" (clj->js {:service service
                                      :query   (normalize-input (:payload data))}))

    (if (normalize-input dd)
      (-> (.loadTable js/imtables
                      target
                      (clj->js {:start 0 :size 10})
                      (clj->js {:service service
                                :query   (normalize-input dd)}))))
    ))

(defn mytable []
  (reagent/create-class
    {:component-did-mount  somefn
     :component-did-update somefn
     :reagent-render       (fn [] [:div [:div.imtable "I AM TABLE"]])}))

(defn ^:export main []
  (fn [{:keys [service payload] :as step-data}]
    [:div [:h1 "Table Component" (str step-data)]]
    [mytable step-data]
    ))