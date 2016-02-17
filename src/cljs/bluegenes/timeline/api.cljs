(ns bluegenes.timeline.api
    (:require [re-frame.core :as re-frame])
    (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(enable-console-print!)

(defn rid []
  (str (make-random-uuid)))


(defn get-id [tool]
  "Returns the ID of a tool or starts a new history and returns the ID if it's not part of a history yet"
  (if (some? (:_id tool))
    (:_id tool)
    (rid)))

(defn append-state [tool data]
  "Append a tool's state to the previous states."
  (re-frame/dispatch [:append-state (keyword (get-id tool)) data]))

(defn replace-state [tool data]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:replace-state (get-id tool) data]))

(defn has-something [tool data]
  "Notify the world that the tool has consumable data."
  (re-frame/dispatch [:has-something (keyword (get-id tool)) data]))

(defn build-api-map
  "Produce a bespoke map of functions for a tool to communicate
  with the framework."
  [step-data]
  {:append-state (partial append-state step-data)
   :replace-state (partial replace-state step-data)
   :has-something (partial has-something step-data)})
