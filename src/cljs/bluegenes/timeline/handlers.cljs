(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v]]
            [bluegenes.db :as db])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(re-frame/register-handler
 :has-something
 trim-v
 (fn [db [step-id data]]
   (let [notify (get-in db [:histories (:active-history db) :steps step-id :notify])]
     ; If our tool has other tools to notify then push the data direclty to them
     (if-not (nil? notify)
       (update-in db [:histories (:active-history db) :steps notify] assoc :input data)
       (update-in db [:histories (:active-history db)] assoc :available-data (assoc data :source {:history (:active-history db)
                                                                                                  :step step-id}))))))
(re-frame/register-handler
  :append-state
  trim-v
  (fn [db [step-id data]]
      (update-in db [:histories (:active-history db) :steps step-id :state] conj data)))

(re-frame/register-handler
  :replace-state
  trim-v
  (fn [db [step-id data]]
      (assoc-in db [:histories (:active-history db) :steps step-id :state] [data])))


(defn rid [] (str (make-random-uuid)))

(defn link-new-step-to-source [db old-step-id new-step-id]
  (update-in db [:histories (:active-history db) :steps old-step-id] assoc :notify new-step-id))

(defn create-step [db id new-step]
  (update-in db [:histories (:active-history db) :steps] assoc id new-step))

(defn clear-data [db]
  (assoc-in db [:histories (:active-history db) :available-data] nil))

(re-frame/register-handler
 :create-next-step
 trim-v
 (fn [db [tool-name]]
   (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
         source (:source last-emitted)
         data (:data last-emitted)
         uuid (keyword (rid))]
     (clear-data (link-new-step-to-source (create-step db uuid {:tool        tool-name
                                                    :uuid        uuid
                                                    :title       "No title"
                                                    :description "No contents."
                                                    :has nil
                                                    :input last-emitted
                                                    :settled     true
                                                    :state       []})
                              (:step source)
                              uuid)))))
