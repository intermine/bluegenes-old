(ns bluegenes.database.core
  (:require [bluegenes.mounts :refer [database]]
            [clojure.data.json :as json]
            [monger.core :as mg]
            [monger.collection :as mc]
            [taoensso.timbre :as timbre :refer (log debug  info  error)])
  (:import org.bson.types.ObjectId))

(defn uuid [] (str (java.util.UUID/randomUUID)))


(defn create-user [{:keys [email name password]}]
  ; Mock a mock user in the database
  (mc/insert database "users" {:email email
                               :name name
                               :password password}))
