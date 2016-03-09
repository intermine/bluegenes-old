(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [bluegenes.utils.machinefields :as machine]
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
  (println "Rows query sees maps" query-map)
  (let [c (chan)]
    (println "Rows: in the let" (clj->js service))
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
   (go (let [response (<! (http/get (str "http://" (.-root service) "/service/model/" type "." (clj->js k)) {:with-credentials? false}))]
   (-> response :body))))

(defn get-display-names [service type response]
  (doall (for [[k v] response]
    (if (is-good-result? k v)
      (go (let [display-name (<! (get-display-name service type k))]
      ))))))

(defn map-response [response]
  "formats the map response for easier updating"
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

            (get-display-names svc type response)

            (>! c mapped-response))))))))
        (fn [error]
          (println "got error" error)
          )))
    c))
