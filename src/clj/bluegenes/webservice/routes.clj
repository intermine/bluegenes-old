(ns bluegenes.webservice.routes
  (:require [compojure.core :refer [GET defroutes context ANY]]
            [ring.util.response :refer [response]]
            [bluegenes.webservice.handlers :as handlers]))

(defroutes auth
  (GET "/google/authenticate/:token" [token](response (handlers/google-auth-handler token)))
  (GET "/google/credentials" [token] (response (handlers/google-client-credentials))))

(defroutes history
  (GET "/" [] (response (handlers/histories)))
  (GET "/:id" [id] (response (handlers/history-summary id)))
  (GET "/:id/details" [id] (response (handlers/history-details id))))

(defroutes routes
  (GET "/version" [] (response {:version "0.0.1"}))
  (context "/auth" [] auth)
  (context "/history" [] history))
