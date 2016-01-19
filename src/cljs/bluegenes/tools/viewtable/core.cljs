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
            :code "A"
            }]})

(defn get-id-query [service list]
  {:from "Gene"
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"
            }]})

(defn table [service-in query-in]
  (let [selector "#some-elem"
        service (clj->js service-in)
        query (clj->js query-in)]
    (-> (.loadTable js/imtables
                    selector
                    #js {:start 0 :end 5}
                    #js {:service service :query query}))) nil)

(defn ^:export main []
  (fn [input]
    (let [query (cond
                  (= "list" (-> input :input :data :format))
                  (get-list-query (get-in input [:input :service]) (get-in input [:input :data]))
                  (= "ids" (-> input :input :data :format))
                  (get-id-query (get-in input [:input :service]) (get-in input [:input :data])))]
      [:div
       [:div#some-elem]
       (table (get-in input [:input :service]) query)])))
