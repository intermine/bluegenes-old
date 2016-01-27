(ns bluegenes.tools.viewtable.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imtables :as imtables]
            [intermine.imjs :as imjs]))

(def search-results (reagent.core/atom {:results nil}))

(defn get-list-query [service list]
  {:from (:type list)
   :select "*"
   :where [{:path (:type list)
            :op "IN"
            :value (:name list)
            :code "A"}]})

(defn get-id-query [service list]
  {:from "Gene"
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"}]})

(defn table [el-id service-in query-in responder]
  (let [selector (str "#" (name el-id))
        service (clj->js service-in)
        query (clj->js query-in)]
    (-> (.loadTable js/imtables
                    selector
                    (clj->js {:start 0 :size 5})
                    (clj->js {:service service :query query}))
        (.then (fn [e]
                 (responder {:service {:root "www.flymine.org/query"}
                             :data {:format "query"
                                    :type (-> e .-query .-root)
                                    :value (js->clj (-> e .-query .toJSON))}}))))))

(defn ^:export main []
  (fn [input comms]
    (reagent/create-class
     {:reagent-render (fn []
                        [:div
                         [:div {:id (str "z" (name (:uuid input)))}]])
      :component-did-mount (fn [comp]
                             (let [input (reagent/props comp)
                                   query (cond
                                           (= "list" (-> input :input :data :format))
                                           (get-list-query (get-in input [:input :service]) (get-in input [:input :data]))
                                           (= "ids" (-> input :input :data :format))
                                           (get-id-query (get-in input [:input :service]) (get-in input [:input :data]))
                                           (= "query" (-> input :input :data :format))
                                           (get-in input [:input :data :value]))]
                               (table (str "z" (name (:uuid input))) (get-in input [:input :service]) query (get comms :has-something))))

      :component-did-update (fn [comp]
                             (let [input (reagent/props comp)
                                   query (cond
                                           (= "list" (-> input :input :data :format))
                                           (get-list-query (get-in input [:input :service]) (get-in input [:input :data]))
                                           (= "ids" (-> input :input :data :format))
                                           (get-id-query (get-in input [:input :service]) (get-in input [:input :data]))
                                           (= "query" (-> input :input :data :format))
                                           (get-in input [:input :data :value]))]
                               (table (str "z" (name (:uuid input))) (get-in input [:input :service]) query (get comms :has-something))))})))
