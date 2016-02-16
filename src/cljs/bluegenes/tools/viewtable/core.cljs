(ns bluegenes.tools.viewtable.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imtables :as imtables]
            [intermine.imjs :as imjs]))

(enable-console-print!)

(defn get-list-query
  "Construct a query using a collection of object ids."
  [list]
  {:from (:type list)
   :select "*"
   :where [{:path (:type list)
            :op "IN"
            :value (:name list)
            :code "A"}]})

(defn get-id-query
  "Construct a query using an intermine list name."
  [list]
  {:from (:type list)
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"}]})

(defn table
  "Render an im-table."
  [el-id service-in query-in has-something]
  (let [selector (str "#" (name el-id))
        service (clj->js service-in)
        query (clj->js query-in)]
    [:h1 "disabled"]
    (-> (.loadTable js/imtables
                    selector
                    (clj->js {:start 0 :size 5})
                    (clj->js {:service service :query query}))
        (.then (fn [e]
                 (has-something {:service service
                                 :data {:format "query"
                                        :type (-> e .-query .-root)
                                        :value (js->clj (-> e .-query .toJSON))}}))))))

(defn normalize-input
  "Convert a variety of inputs into an imjs compatible clojure map."
  [input-data]
  (cond
    (= "list" (-> input-data :data :format))
    (get-list-query (get-in input-data [:data]))
    (= "ids" (-> input-data  :data :format))
    (get-id-query (get-in input-data [:data]))
    (= "query" (-> input-data :data :format))
    (get-in input-data [:data :value])))

(defn updater
  "(Re)render an inner im-table component."
  [comp]
  (let [{:keys [state upstream-data api]} (reagent/props comp)]
    (let [query (normalize-input upstream-data)]
      (table "z" (-> upstream-data :service) query (:has-something api)))))


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
      [:h4 (str @state " rows")])))


(defn ^:export main
  "Render the main view of the tool."
  []
  (reagent/create-class
   {:reagent-render (fn []
                      [:div {:id (str "z")}])
    :component-did-mount (fn [comp]
                           (updater comp))
    :component-did-update (fn [comp]
                            (updater comp))}))
