(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [bluegenes.utils.machinefields :as machine]
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
  [ {{:keys [root token]} :service} {:keys [ids list widget maxp correction population]}]
  (println "ids" ids)
  (go (let [response (<! (http/post (str "http://" root "/service/list/enrichment")
                                   {:with-credentials? false
                                    :keywordize-keys? true
                                    :form-params (merge
                                                   {:widget widget
                                                    :maxp maxp
                                                    :format "json"
                                                    :correction correction}

                                                   (cond
                                                     ids
                                                     {:ids (clojure.string/join "," ids)}
                                                     list
                                                     {:list list})

                                                   (if-not (nil? population) {:population population}))}))]
        (-> response :body))))


(defn query-rows
  "Returns IMJS row-style result"
  [service query-map]
  (println "Rows query sees maps" query-map)
  (let [c (chan)]
    (println "Rows: in the let" (clj->js service))
    (-> (js/imjs.Service. (clj->js (:service service)))
        (.rows (clj->js query-map))
        (.then (fn [rows]
                 (go (>! c (js->clj rows :keywordize-keys true))))
               (fn [error])))
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
  (.log js/console "Records query sees maps" (clj->js query-map))
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js (:service service)))
        (.records (clj->js query-map))
        (.then (fn [records]
                 (go (>! c (js->clj records :keywordize-keys true))))
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
             :value id}]})

(defn is-good-result? [k v]
  "Check that values are non null or machine-only names - no point getting dispaly names for them. "
 (and (not (contains? machine/fields k)) ;;don't output user-useless results
 (some? (:val v))) ;;don't output null results
 )

 (defn get-display-name [service type k]
   "Given a service URL, a type to search for, and an attribute field, return the display name."
   (go (let [response (<! (http/get (str "http://" (:root service) "/service/model/" type "." (clj->js k)) {:with-credentials? false :keywordize-keys true}))]
   (-> response :body :name))))

(defn map-response [response]
  "formats the summary fields map response for easier updating"
  (reduce (fn [new-map [k v]]
    (assoc new-map k {:name k :val v}))
        {} response))

(defn summary-fields
  "Returns summary fields of a given ID. requires service in the format {:service {:root 'http://www.someintermine.org/query' :token 'token if any'}}"
  [service type id]
  (println "summary field sees type id" type id)
  (let [c (chan) svc (clj->js (:service service))]
    (-> (js/imjs.Service. svc)
      (.makePath (clj->js type))
      (.then (fn [result]

    (->
      (.getDisplayName result)
      (.then (fn [displayname]

         (let [q (summary-query type id (.allDescriptors result))]
          (go (let [response (first (<! (query-records service q)))
                    mapped-response (map-response response)]
            (>! c mapped-response))))))))
        (fn [error]
          (println "got error" error)
          )))
    c))
