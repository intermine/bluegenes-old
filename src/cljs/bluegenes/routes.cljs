(ns bluegenes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
            (re-frame/dispatch [:set-active-panel :home-panel]))

  (defroute "/about" []
            (re-frame/dispatch [:set-active-panel :about-panel]))


  (defroute "/timeline/:id" [id]
            (do
              (re-frame/dispatch [:set-timeline-panel :timeline-panel id])))

  (defroute "/timeline" []
            (re-frame/dispatch [:set-active-panel :timeline-panel]))



  (defroute "/debug" []
            (re-frame/dispatch [:set-active-panel :debug-panel]))

  ;; --------------------
  (hook-browser-navigation!))
