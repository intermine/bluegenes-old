(ns bluegenes.tools.showlist.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]))


(def search-results (reagent.core/atom {:results nil}))

; This function works and successfully updates the search-results atom
(defn get-list-contents [service list]
  (let [mine (js/imjs.Service. (clj->js service))]
    (-> mine
        (.records (clj->js {:from (:type list)
                              :select "*"
                              :where [{
                                       :path (:type list)
                                       :op "IN"
                                       :value (:name list)
                                       :code "A"
                                       }]}))
        (.then (fn [results]
                 (swap! search-results assoc :results results))))))


(defn get-id-contents [service list]
  (let [mine (js/imjs.Service. (clj->js service))]
    (-> mine
        (.records (clj->js {:from "Gene"
                            :select "*"
                            :where [{
                                     :path "Gene.id"
                                     :values (:values list)
                                     :op "ONE OF"
                                     :code "A"
                                     }]}))
        (.then (fn [results]
                 (.log js/console "Results" results)
                 (swap! search-results assoc :results results))))))

; never re-renders
(defn inner []
  (fn []
    [:ul
     (for [result (:results @search-results)]
       [:li (.-primaryIdentifier result)])]))

(defn main []
  (fn [input]
    (if (= "list" (-> input :input :data :format))
      (get-list-contents (get-in input [:input :service]) (get-in input [:input :data])))
    (if (= "ids" (-> input :input :data :format))
      (get-id-contents (get-in input [:input :service]) (get-in input [:input :data])))
    [:div
     ^{:key (get-in input [:input :data :name])} [inner]]))
