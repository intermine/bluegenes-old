(ns bluegenes.webservice.handlers
  (:use ring.middleware.json
        [taoensso.timbre :only (info debug)])
  (:require [clj-http.client :as client]
            [bluegenes.db.users :as users]
            [bluegenes.db.histories :as histories]
            [cheshire.core :as cheshire]
            [environ.core :refer [env]]
            [clj-http.client :as client])
  (:gen-class))

(defn google-auth-handler [token]
  (let [res (client/post "https://www.googleapis.com/oauth2/v3/tokeninfo"
                         {:form-params {:id_token token}})
        parsed-body (cheshire/parse-string (:body res))]
    (if (nil? (users/find-user-by-email (get "email" parsed-body)))
      (users/create-user parsed-body)
      (users/find-user-by-email (get "email" parsed-body)))))

(defn google-client-credentials []
  {:client-id (env :google-client-id)})

(defn histories []
  ; (histories/get-all-histories)
  (histories/get-all-histories-with-steps)
  )

(defn history-summary [id]
  (histories/get-history id))

(defn history-details [id]
  (histories/get-history-with-steps id))
