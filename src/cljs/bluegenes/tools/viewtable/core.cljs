(ns bluegenes.tools.viewtable.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imtables :as imtables]
            [reagent.impl.util :as impl :refer [extract-props]]
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

(defn inner-table
  "Renders an im-table"
  []
  (let [update-table (fn [comp]
                       (let [{:keys [state upstream-data api]} (reagent/props comp)
                             node (reagent/dom-node comp)
                             target  (.item (.getElementsByClassName node "imtable") 0)
                             query (normalize-input upstream-data)]
                         (-> (.loadTable js/imtables
                                         target
                                         (clj->js {:start 0 :size 5})
                                         (clj->js {:service (:service upstream-data) :query query}))
                             (.then
                              (fn [e]
                                (let [clone (.clone (-> e .-query))
                                      adj (.select clone #js [(str (-> e .-query .-root) ".id")])]
                                  (-> (js/imjs.Service. (clj->js (:service upstream-data)))
                                      (.values adj)
                                      (.then (fn [v]
                                               ((:has-something api) {:service (:service upstream-data)
                                                                      :data {:format "ids"
                                                                             :type (-> e .-query .-root)
                                                                             :values (js->clj v)}}))))))))))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:div
                         [:div.imtable]])
      :component-did-update update-table
      :component-did-mount update-table})))


(defn ^:export main
  "Render the main view of the tool."
  []
  (reagent/create-class
   {:reagent-render (fn [props]
                      [inner-table props])
    :should-component-update (fn [this old-argv new-argv]
                               (not (=
                                     (dissoc (extract-props old-argv) :api)
                                     (dissoc (extract-props new-argv) :api))))}))
