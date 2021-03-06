(ns bluegenes.timeline.api
    (:require [re-frame.core :as re-frame])
    (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

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

(defn update-state [tool f]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:update-state (get-id tool) f]))

(defn has-something [tool data]
  "Notify the world that the tool has consumable data."
  (re-frame/dispatch [:has-something (keyword (get-id tool)) data]))

(defn is-loading [tool data]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:is-loading (get-id tool) data]))

(defn build-api-map
  "Produce a bespoke map of functions for a tool to communicate
  with the framework."
  [step-data]
  {:append-state (partial append-state step-data)
   :update-state (partial update-state step-data)
   :replace-state (partial replace-state step-data)
   :has-something (partial has-something step-data)
   :is-loading (partial is-loading step-data)})

(defn append-state-or-new-history
  "same as append state, but creates the history first from a homepage tool if it doesn't exist"
  [tool data]
  (let [active-history (re-frame/subscribe [:active-history])]
    (if (nil? @active-history)
      (re-frame/dispatch [:start-new-history tool data])
      (append-state tool data))))


(defn replace-state-or-new-history
  "same as replace state, but creates the history first from a homepage tool if it doesn't exist"
  [tool data]
  (let [active-history (re-frame/subscribe [:active-history])]
    (if (nil? @active-history)
      (re-frame/dispatch [:start-new-history tool data])
      (replace-state tool data))
  ))

(defn build-homepage-api-map
  "Produce a bespoke map of functions for a tool to communicate
  with the framework."
  [step-data]
  {:append-state (partial append-state-or-new-history step-data)
   :replace-state (partial replace-state-or-new-history step-data)
   :has-something (partial has-something step-data)})

(defn has-something-list-entry [tool data]
 "Notify the world that the tool has consumable data."
 (re-frame/dispatch [:list-entry-has-something (keyword (get-id tool)) data]))


(defn build-list-entry-api-map
 "Produce a bespoke map of functions for a tool to communicate
 with the framework."
 [step-data]
 {:append-state (partial append-state step-data)
  :replace-state (partial replace-state step-data)
  :has-something (partial has-something-list-entry step-data)
  :is-loading (partial is-loading step-data)})
