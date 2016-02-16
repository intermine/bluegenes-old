(ns bluegenes.tools.viewtable.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imtables :as imtables]
            [intermine.imjs :as imjs]))

(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))

(defn get-list-query [list]
  {:from (:type list)
   :select "*"
   :where [{:path (:type list)
            :op "IN"
            :value (:name list)
            :code "A"}]})

(defn get-id-query [list]
  {:from (:type list)
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"}]})

(defn table [el-id service-in query-in has-something]
  (let [selector (str "#" (name el-id))
        service (clj->js service-in)
        query (clj->js query-in)]
    [:h1 "disabled"]
    (-> (.loadTable js/imtables
                    selector
                    (clj->js {:start 0 :size 5})
                    (clj->js {:service service :query query}))
        (.then (fn [e]
                 (has-something {:service {:root "www.flymine.org/query"}
                             :data {:format "query"
                                    :type (-> e .-query .-root)
                                    :value (js->clj (-> e .-query .toJSON))}}))))))

(defn updater [comp]
  (let [{:keys [state upstream-data api]} (reagent/props comp)]
    (let [query (cond
                  (= "list" (-> upstream-data :data :format))
                  (get-list-query (get-in upstream-data [:data]))
                  (= "ids" (-> upstream-data  :data :format))
                  (get-id-query (get-in upstream-data [:data]))
                  (= "query" (-> upstream-data :data :format))
                  (get-in upstream-data [:data :value]))]
      (table "z" (-> upstream-data :service) query (:has-something api)))))


(defn update-count [comp state]
  (let [upstream-data comp]
    (let [query (cond
                  (= "list" (-> upstream-data :data :format))
                  (get-list-query (get-in upstream-data [:data]))
                  (= "ids" (-> upstream-data  :data :format))
                  (get-id-query (get-in upstream-data [:data]))
                  (= "query" (-> upstream-data :data :format))
                  (get-in upstream-data [:data :value]))]

      (-> (js/imjs.Service. (clj->js (:service upstream-data)))
          (.query (clj->js query))
          (.then (fn [q] (.count q)))
          (.then (fn [c]
                   (reset! state c)))))))


(defn ^:export preview []
  (let [state (reagent/atom 0)]
    (fn [data]
      (update-count data state)
      [:h4 (str @state " rows")])))


(defn ^:export main []
  (reagent/create-class
   {:reagent-render (fn []
                      [:div {:id (str "z")}])
    :component-did-mount (fn [comp]
                           (updater comp))
    :component-did-update (fn [comp]
                            (updater comp))}))
