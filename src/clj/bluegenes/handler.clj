(ns bluegenes.handler
  (:require [compojure.core :refer [GET defroutes]]
            [ring.util.response :refer [file-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defroutes handler
  (GET "/" [] (file-response "index.html" {:root "resources/public"})))

(def app (wrap-resource handler "public"))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (println (format "Starting web server on port %d." port))
    (run-jetty app {:port port})))

(defn run [& [port]]
  (run-web-server port))

(defn -main [& [port]]
  (run port))
