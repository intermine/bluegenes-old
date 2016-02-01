(ns bluegenes.webservice.history.handlers
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [schema.core :as s]
            [slugger.core :as slugger]
            [monger.operators :refer :all]
            [bluegenes.mounts :refer [database]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn get-all-histories []
  (mc/find-maps database "histories"))


(defn add-step
  "Add a new step to a history."
  [history-id input]
  (let [step-id (uuid)]
    (mc/update database "histories" {:_id history-id}
               {$set {(str "steps." step-id) input}})
    step-id))

(defn update-step
  "Add a step to a history."
  [{:keys [history-id step-id] :as input}]
  (let []
    (mc/update database "histories" {:_id history-id}
               {$set {(str "steps." step-id) input}})))

(defn create-history [{:keys [name description]}]
  (mc/insert-and-return database "histories" {:_id (uuid)
                                              :name name
                                              :slug (slugger/->slug name)
                                              :description description}))
