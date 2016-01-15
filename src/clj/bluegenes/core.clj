(ns bluegenes.core
  (:require [bluegenes.database.core :as database]
            [taoensso.timbre :as timbre :refer (info)]
            [mount.core :as mount]))

(defn -main []
  (info "Starting Bluegenes")
  (mount/start)
  (database/create-user {:name "josh" :email "josh@example.com" :password "test"}))
