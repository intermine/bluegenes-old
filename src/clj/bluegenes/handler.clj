(ns bluegenes.handler
  (:use ring.middleware.json)
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [file-response response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [bluegenes.webservice.handler :as api]
            [bluegenes.mounts :as mounts]
            [bluegenes.utils :as utils]
            [mount.core :as mount])
  (:gen-class))



(mount/start)
(utils/seed)

(defroutes app-routes
  (GET "/" [] (file-response "index.html" {:root "resources/public"}))
  (context "/api" [] api/api-routes)
  (route/not-found
    (response {:message "Page not found2"})))

(defn wrap-log-request [handler]
  (fn [req]
    (println req)
    (handler req)))

(def app
  (-> app-routes
    wrap-log-request
    wrap-json-response
    wrap-json-body))



; (defroutes handler
;   (context "/api" [] api/routes)
;   (GET "/" [] (file-response "index.html" {:root "resources/public"})))
;
; ; (def app (wrap-resource handler "public"))
; (def app (-> (wrap-resource handler "public")
;              (middleware/wrap-json-body)
;              (middleware/wrap-json-response)))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (println (format "Starting web server on port %d." port))
    (run-jetty app {:port port})))

(defn run [& [port]]
  (run-web-server port))

(defn -main [& [port]]
  (run port))
