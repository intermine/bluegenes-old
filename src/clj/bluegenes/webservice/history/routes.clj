(ns bluegenes.webservice.history.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [ring.util.response :refer [response]]
            [bluegenes.webservice.history.handlers :as handlers]))


(defroutes root
  (GET "/" [] (response (handlers/get-all-histories)))
  (GET "/step/:id" [id] (response (handlers/add-step {:history-id id
                                                :notifier-id "notifier123"
                                                :tool "choosetemplate"
                                                :state nil
                                                :description "banana"})))
  (GET "/:id" [id] (response (handlers/create-history {:name "test history"
                                                       :description "hello josh"}))))
