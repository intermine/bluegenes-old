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
  (println "URL" (str "http://" root "/service/list/enrichment"))
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
;  (.log js/console "Records query sees maps" (clj->js query-map))
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

(defn map-summary-response [response]
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
                    mapped-response (map-summary-response response)]
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

(defn local-homologue-query [ids type organism]
    {
    :from type
    :select [
      "primaryIdentifier"
      "symbol"
      "organism.shortName"
      ]
    :orderBy [
        {
        :path "primaryIdentifier"
        :direction "ASC"
        }
      ]
    :where [
        {
        :path type
        :op "LOOKUP"
        :value ids
        :extraValue organism
        }
      ]
    })

(defn map-local-homologue-response [data]
  "formats the get-local-homologues response to match the default homologue response shape, so they can be output using the same logic."
  {:homologues (map (fn [homie] {:homologue homie} ) data)})

(defn get-local-homologues  [original-service remote-service q type organism]
  "If the remote mine says it has no homologues for a given identifier, query the local mine instead. It may be that there *are* homologues, but the remote mine doesn't know about them. If the local mine returns identifiers, verify them on the remote server and return them to the user."
  (let [c (chan)]
    ;(.log js/console "%c getting local homologues for %s" "border-bottom:wheat solid 3px" (:root (:service remote-service)))
    (go (let [
      ;;get the list of homologues from the local mine
      local-homologue-results (:homologues (first (<! (query-records original-service q))))]
        (if (some? local-homologue-results)
          (do (let
            ;;convert the results to just the list of homologues
            [local-homologue-list (map #(-> % :homologue :primaryIdentifier) local-homologue-results)
            ;;build the query to send to the remote service
            remote-homologue-query (local-homologue-query local-homologue-list type organism)
            ;;look up the list of identifers we just made on the remote mine to
            ;;get the correct objectid to link to
            remote-homologue-results (<! (query-records remote-service remote-homologue-query))]
            ;;put the results in the channel
            (>! c (map-local-homologue-response remote-homologue-results))))
            (>! c {:homologues [] })))) c))

(defn homologues
  "returns homologues of a given gene id from a remote mine."
  [original-service remote-service type id organism]
  (let [c (chan)]
    (go (let [
      ;;get the primary identifier from the current mine
      primary-id (<! (get-primary-identifier type id original-service))
      ;build the query
      q (homologue-query primary-id organism)
      ;;query the remote mine for homologues
      response (<! (query-records remote-service q))]
          ;(.log js/console "%c getting homologues for %s" "border-bottom:mediumorchid dotted 3px" (:root (:service remote-service)) (clj->js response))
      (if (> (count response) 0)
        (>! c (first response))
        (>! c (<! (get-local-homologues original-service remote-service q type organism)))
)))c))

(defn templates [service]
 "Fetch templates from Intermine and return them over a channel"
 (let [templates-chan (chan)]
   (-> (js/imjs.Service. (clj->js service))
       (.fetchTemplates)
       (.then (fn [response]
                (go (>! templates-chan (js->clj response :keywordize-keys true))))))
   templates-chan))

(defn model [service]
 "Given a service URL, a type to search for, and an attribute field, return the display name."
 (go (let [response (<! (http/get (str "http://" (:root service) "/service/model/json") {:with-credentials? false :keywordize-keys true}))]
       (-> response :body :model :classes))))
