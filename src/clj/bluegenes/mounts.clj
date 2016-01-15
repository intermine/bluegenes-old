(ns bluegenes.mounts
  (:require [clojure.data.json :as json]
            [monger.core :as mg]
            [monger.collection :as mc]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as timbre :refer (log debug  info  error)])
  (:import org.bson.types.ObjectId))

(defn connect-db []
    (info "Connecting to database.")
    (mg/get-db (mg/connect) "bluegenes"))

(defn disconnect-db [conn]
  (info "Disconnecting from database.")
  (mg/disconnect conn))

(defstate database
  :start (connect-db)
  :stop (disconnect-db database))
