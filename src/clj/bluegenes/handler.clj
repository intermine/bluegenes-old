(ns bluegenes.handler
  (:use ring.middleware.json
        [taoensso.timbre :only (info debug)])
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.handler :as handler]
            [ring.util.response :refer [file-response response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [mount.core :as mount]
            [bluegenes.webservice.routes :as webservice])
  (:gen-class))

(mount/start)

(defn wrap-log-request [handler]
  (fn [req]
    (info req)
    (handler req)))

(defroutes app-routes
  (GET "/" [] (file-response "index.html" {:root "resources/public"}))
  (context "/api" [] webservice/routes))

(def app
  (-> app-routes
    (wrap-resource "public")
    ; wrap-log-request
    wrap-json-response
    wrap-json-body))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (info (format "Starting web server on port %d." port))
    (run-jetty app {:port port})))

(defn run [& [port]]
  (run-web-server port))

(defn -main [& [port]]
  (run port))
