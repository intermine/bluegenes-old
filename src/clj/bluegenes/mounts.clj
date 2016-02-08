(ns bluegenes.mounts
  (:require [mount.core :refer [defstate]]
            [monger.core :as mg]
            [environ.core :refer [env]]))

(defn dbstart
  "Open a connection to MongoDB using the environment variable MONGO_URL"
  []
  (let [{:keys [conn db]} (mg/connect-via-uri (env :mongo-url))]
    db))

(defstate database
  :start (dbstart)
  :stop nil)
