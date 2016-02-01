(ns bluegenes.mounts
  (:require [mount.core :refer [defstate]]
            [monger.core :as mg]))


(defn dbstart[]
  (let [conn (mg/connect)
        db   (mg/get-db conn "monger-test")
        coll "documents"]
    db))


(defstate database
  :start (dbstart)
  :stop nil)
