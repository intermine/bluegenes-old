(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [bluegenes.utils.machinefields :as machine]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [intermine.imjs :as imjs]))

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

(defn get-primary-identifier
  "Returns the primary identifier associated with a given object id. Useful for cross-mine queries, as object ids aren't consistent between different mine instances."
  [type id service]
    (let [c (chan) q {
      :from type
      :select "primaryIdentifier"
      :where [{
        :path (str type ".id")
        :op "="
        :value id}]}]
      (go (let [response (<! (query-records service q))]
        (>! c (:primaryIdentifier (first response)))))
      c))

(defn homologue-query [id organism]
  {
  :constraintLogic "A and B"
  :from "Gene"
  :select [
    "homologues.homologue.primaryIdentifier"
    "homologues.homologue.symbol"
    "homologues.homologue.organism.shortName"
    ]
  :orderBy [
      {
      :path "primaryIdentifier"
      :direction "ASC"
      }
    ]
  :where [
      {
      :path "primaryIdentifier"
      :op "="
      :value id
      :code "A"
      }
      {
      :path "homologues.homologue.organism.shortName"
      :op "="
      :value organism
      :code "B"
      }
    ]
  })

(defn homologues
  "returns homologues of a given gene id from a remote mine."
  [original-service remote-service type id organism]
  (let [c (chan)]
    (go (let [
            ;;get the primary identifier from the current mine
            primary-id (<! (get-primary-identifier type id original-service))
            ;;build the query
            q (homologue-query primary-id organism)
            ;;query the remote mine for homologues
            response (<! (query-records remote-service q))]
            (.log js/console "%c Homologues" "border-bottom:mediumorchid dotted 3px" (clj->js response)) ;;this prints the expected response

            (>! c response) ;; put the response in the channel
    ))c))
