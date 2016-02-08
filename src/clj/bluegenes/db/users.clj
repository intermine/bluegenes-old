(ns bluegenes.db.users
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]
            [bluegenes.mounts :refer [database]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn create-user [props]
  (mc/insert-and-return database "users" (merge props {:_id (uuid)
                                                       :token (uuid)
                                                       :persistent true})))

(defn find-user-by-email [email]
  (mc/find-one-as-map database "users" {:_email email}))
