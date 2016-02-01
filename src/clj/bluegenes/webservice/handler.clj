(ns bluegenes.webservice.handler
  (:use ring.middleware.json)
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [bluegenes.webservice.history.routes :as history-routes]))


(defroutes api-routes
  (context "/history"[] history-routes/root)
  (GET "/version" [] (response {:version "0.0.1"})))


; (defroutes app-routes
;   (GET "/" [] (file-response "index.html" {:root "resources/public"}))
;   (route/not-found
;     (response {:message "Page not found2"})))
;
; (defn wrap-log-request [handler]
;   (fn [req]
;     (println req)
;     (handler req)))
;
; (def app
;   (-> app-routes
;     wrap-log-request
;     wrap-json-response
;     wrap-json-body))
;
;
;
; ; (defroutes handler
; ;   (context "/api" [] api/routes)
; ;   (GET "/" [] (file-response "index.html" {:root "resources/public"})))
; ;
; ; ; (def app (wrap-resource handler "public"))
; ; (def app (-> (wrap-resource handler "public")
; ;              (middleware/wrap-json-body)
; ;              (middleware/wrap-json-response)))
;
; (defn run-web-server [& [port]]
;   (let [port (Integer. (or port (env :port) 5000))]
;     (println (format "Starting web server on port %d." port))
;     (run-jetty app {:port port})))
;
; (defn run [& [port]]
;   (run-web-server port))
;
; (defn -main [& [port]]
;   (run port))
