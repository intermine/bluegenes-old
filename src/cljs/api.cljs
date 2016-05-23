(ns bluegenes.api
  (:require [re-frame.core :as re-frame]))

(defn has-something [location data]
  (println "has-something called with data" data)
  (re-frame/dispatch [:has-something location data]))

(defn save-state [location data]
  (println "save-state called with data" data)
  (re-frame/dispatch [:save-state location data]))

(defn save-cache [location data]
  ;(println "save-state called with data" data)
  (re-frame/dispatch [:save-cache location data]))

(def api {:has-something (partial has-something :someid)})

