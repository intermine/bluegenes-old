(ns bluegenes.tools.viewtable.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imtables :as imtables]
            [intermine.imjs :as imjs]))

(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))

(defn get-list-query [service list]
  {:from (:type list)
   :select "*"
   :where [{:path (:type list)
            :op "IN"
            :value (:name list)
            :code "A"}]})

(defn get-id-query [service list]
  {:from (:type list)
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"}]})

(defn table [el-id service-in query-in responder]
  (let [selector (str "#" (name el-id))
        service (clj->js service-in)
        query (clj->js query-in)]
    [:h1 "disabled"]
    (-> (.loadTable js/imtables
                    selector
                    (clj->js {:start 0 :size 5})
                    (clj->js {:service service :query query}))
        (.then (fn [e]
                 (responder {:service {:root "www.flymine.org/query"}
                             :data {:format "query"
                                    :type (-> e .-query .-root)
                                    :value (js->clj (-> e .-query .toJSON))}}))))))

(defn updater [comp]
  (let [{:keys [state upstream-data api]} (reagent/props comp)]
    (println "updater sees" upstream-data)
    (let [query (cond
                  (= "list" (-> upstream-data :data :format))
                  (get-list-query (get-in upstream-data [:service]) (get-in upstream-data [:data]))
                  (= "ids" (-> upstream-data  :data :format))
                  (get-id-query (get-in upstream-data [:service]) (get-in upstream-data [:data]))
                  (= "query" (-> upstream-data :data :format))
                  (get-in upstream-data [:data :value]))]
      (table "z" (-> upstream-data :service) query (:has-something api)))))

(defn ^:export main []
  (reagent/create-class
   {:reagent-render (fn []
                      [:div {:id (str "z")}])
    :component-did-mount (fn [comp]
                           (updater comp))
    :component-did-update (fn [comp]
                            (updater comp))}))
