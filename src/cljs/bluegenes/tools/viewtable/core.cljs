(ns bluegenes.tools.viewtable.core
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

(defn update-count
  "Reset an atom with the row count of an imjs query."
  [input-data state]
  (let [query (normalize-input input-data)]
    (-> (js/imjs.Service. (clj->js (:service input-data)))
        (.query (clj->js query))
        (.then (fn [q] (.count q)))
        (.then (fn [c]
                 (reset! state c))))))

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


(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache]}]
  (if input
    (save-state {:service (:service input)
                 :data    {:payload (normalize-input input)
                           :format  "query"
                           :type    "Gene"}})))

(defn somefn [component]
  (let [{:keys [state api]} (reagent/props component)
        node (reagent/dom-node component)
        target (.item (.getElementsByClassName node "imtable") 0)]
    (-> (.loadTable js/imtables
                    target
                    (clj->js {:start 0 :size 10})
                    (clj->js {:service (:service state) :query (:payload (:data state))}))
        (.then (fn [table]
                 (-> table .-history (.on "changed:current"
                                          (fn [x]
                                            ((:save-state api)
                                              {:service (:service state)
                                               :data    {:payload (js->clj (-> x .-attributes .-query (.toJSON)) :keywordize-keys true)
                                                         :format  "query"
                                                         :type    "Gene"}})))))
               (fn [error] (println "TABLE ERROR" error))))))

(defn mytable []
  (reagent/create-class
    {:component-did-mount  somefn
     :component-did-update somefn
     :reagent-render       (fn [] [:div [:div.imtable "I AM TABLE"]])}))

(defn ^:export main []
  (fn [{:keys [state api] :as step-data}]
    [mytable (select-keys step-data [:api :state])]))