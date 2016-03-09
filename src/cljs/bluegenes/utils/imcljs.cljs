(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [intermine.imjs :as imjs]))

(defn resolve-ids
  "Completes the steps required to resolve identifiers.
  1. Start an ID Resolution job.
  2. Poll the server for the job status (every 1s)
  3. Delete the job (side effect).
  4. Return results"
  [{{:keys [root token]} :service}
   input]
  (go (let [response (<! (http/post (str "http://" root "/service/ids")
                                    {:with-credentials? false
                                     :json-params (clj->js input)}))]
        (if-let [uid (-> response :body :uid)]
          (loop []
            (let [status-response (<! (http/get (str "http://" root "/service/ids/" uid "/status")
                                                {:with-credentials? false}))]
              (if (= "SUCCESS" (:status (:body status-response)))
                (let [final-response (<! (http/get (str "http://" root "/service/ids/" uid "/results")
                                                   {:with-credentials? false}))]
                  (http/delete (str "http://" root "/service/ids/" uid)
                               {:with-credentials? false})
                  final-response)
                (do
                  (<! (timeout 1000))
                  (recur)))))))))


(defn enrichment
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [ {{:keys [root token]} :service} {:keys [list widget maxp correction population]}]
  (go (let [response (<! (http/get (str "http://" root "/service/list/enrichment")
                                   {:with-credentials? false
                                    :keywordize-keys? true
                                    :query-params (merge {:list list
                                                          :widget widget
                                                          :maxp maxp
                                                          :format "json"
                                                          :correction correction}

                                                         (if-not (nil? population) {:population population}))}))]
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



(defn lists
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [{{:keys [root token]} :service}]
  (go (let [response (<! (http/get (str "http://" root "/service/lists")
                                   {:with-credentials? false
                                    :keywordize-keys? true}))]
        (-> response :body :lists))))

(defn query-records
  "Returns an IMJS records-style results"
  [service query-map]
  (.log js/console "query sees maps" (clj->js query-map))
  (let [c (chan)]
    (println "in the QSM let" (clj->js service))
    (-> (js/imjs.Service. (clj->js (:service service)))
        (.records (clj->js query-map))
        (.then (fn [rows]
                 (println "got the rows" rows)
                 (go (>! c (js->clj rows :keywordize-keys true))))
               (fn [error]
                 (.log js/console "got error" error)
                 )))
    c))

(defn all-attributes [path-info]
  "returns attribute list for a given path"
  (clj->js
    (keys
      (js->clj
        (.-attributes
          (first path-info)) :keywordize-keys true))))

(defn summary-query [type id path-info]
  "returns pre-built query object for summary fields"
    {:from type
     :select (all-attributes path-info)
     :where [{
             :path (str type ".id")
             :op "="
             :value id}]}
  )

(defn summary-fields
  "Returns summary fields of a given ID. requires service in the format {:service {:root 'http://www.someintermine.org/query' :token 'token if any'}}"
  [service type id]
  (println "summary field sees type id" type id)
  (let [c (chan) svc (clj->js (:service service))]
    (println "in SUMMARY FIELDS the let" (clj->js service))
    (-> (js/imjs.Service. svc)
      (.makePath (clj->js type))
      (.then (fn [result]

    (->
      (.getDisplayName result)
      (.then (fn [displayname]
        (.log js/console "%c=====" "background:wheat" displayname result))))

         (let [q (summary-query type id (.allDescriptors result))]
          (go (let [response (<! (query-records service q))]
            (>! c response)))))
        (fn [error]
          (println "got error" error)
          )))
    c))
