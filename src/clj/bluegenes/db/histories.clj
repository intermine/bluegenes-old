(ns bluegenes.db.histories
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]
            [slugger.core :as slugger]
            [bluegenes.mounts :refer [database]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn get-all-histories
  "Get all history summaries."
  []
  (mc/find-maps database "histories"))

(defn get-history
  "Get a single history's summary."
  [id]
  (mc/find-one-as-map database "histories" {:_id id}))

(defn get-steps
  "Get steps associated with a history ID."
  [ids]
  (mc/find-maps database "steps" {:_id {$in ids}}))

(defn get-history-with-steps
  "Get a single history and its step details."
  [id]
  (reduce
   (fn [m step-data]
     (update-in m [:steps] assoc
                (keyword (:_id step-data))
                (clojure.set/rename-keys
                 step-data {:_id :id})))
   (assoc (get-history id) :steps {})
   (get-steps (:steps (get-history id)))))

(defn get-all-histories-with-steps
  "Get all histories and their step details."
  []
  (map #(get-history-with-steps (:_id %)) (get-all-histories)))

(defn map->dot
  "Convert a clojure map to mongo dot notation with the option to prepend
  a string. Example: {:val1 1 :val2 2} with 'step' as the option argument
  produces {step.val1 1 step.val2 2}"
  [m & [prepend-str]]
  (into {} (map (fn [[k v]] [(str (if-not (nil? prepend-str)
                                    (str prepend-str ".")) (name k)) v]) m)))

(defn create-step
  "Create a step. Merges a random UUID on top to prevent hijacking."
  [data]
  (mc/insert-and-return database "steps" (merge data {:_id (uuid)})))

(defn get-id-from-email [email]
  (mc/find-maps database "users" {:email email}))

(defn add-step-to-history
  "Adds an existing step to a history.
  TODO: enforce that user is owner of both the history and the step."
  [history-id step-id]
  (mc/update database "histories" {:_id (name history-id)} {$addToSet {:steps step-id}}))

(defn update-step
  "Update a step with new data using Mongo's non-destructive
  $set operator.
  TODO: Check history for permission to update the step."
  [step-id data]
  (println "updating with steps" data)
  (mc/update database "steps" {:_id step-id} {$set data}))

(defn create-history [{:keys [name description]}]
  (mc/insert-and-return database "histories" {:_id (uuid)
                                              :name name
                                              :steps []
                                              :slug (slugger/->slug name)
                                              :description description}))
