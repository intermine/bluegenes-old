(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [intermine.imjs :as imjs]))

(defn enrichment
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [ {{:keys [root token]} :service} {:keys [list widget maxp correction]}]
  (go (let [response (<! (http/get (str "http://" root "/service/list/enrichment")
                                   {:with-credentials? false
                                    :keywordize-keys? true
                                    :query-params {:list list
                                                   :widget widget
                                                   :maxp maxp
                                                   :format "json"
                                                   :correction correction}}))]
        (-> response :body))))


(defn query-rows
  "Returns IMJS row-style result"
  [service query-map]
  (println "query sees maps" query-map)
  (let [c (chan)]
    (println "in the let" (clj->js service))
    (-> (js/imjs.Service. (clj->js (:service service)))
        (.rows (clj->js query-map))
        (.then (fn [rows]
                 (println "got the rows" rows)
                 (go (>! c (js->clj rows :keywordize-keys true))))
               (fn [error]
                 (println "got error" error)
                 )))
    c))

  (defn query-records
    "Returns an IMJS records-style results"
    [service query-map]
    (println "query sees maps" query-map)
    (let [c (chan)]
      (println "in the let" (clj->js service))
      (-> (js/imjs.Service. (clj->js (:service service)))
          (.records (clj->js query-map))
          (.then (fn [rows]
                   (println "got the rows" rows)
                   (go (>! c (js->clj rows :keywordize-keys true))))
                 (fn [error]
                   (println "got error" error)
                   )))
      c))
