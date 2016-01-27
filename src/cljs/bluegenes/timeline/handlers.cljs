(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v]]
            [bluegenes.db :as db])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(defn get-idx [id steps]
  (loop [needle id
         haystack steps
         pos 0]
    (if (= id (:uuid (nth haystack pos)))
      pos
      (recur id haystack (inc pos)))))

(re-frame/register-handler
  :settle
  trim-v
  (fn [db [data output]]
    (let [finder (fn [idx step] (if (= (:uuid data) (:uuid step))
                              (do
                                (assoc step :settled true :output output))
                              step))]
      db)))

(re-frame/register-handler
  :pass-input trim-v
  (fn [db [data idx]]
    (update-in db [:histories (:active-history db) :steps idx] assoc :input data)))

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

(re-frame/register-handler
  :has-something
  trim-v
  (fn [db [step-id data]]
      (update-in db [:histories (:active-history db)] assoc :available-data (assoc data :source {:history (:active-history db)
                                                                                                 :step step-id}))))
(defn rid [] (str (make-random-uuid)))

(defn link-new-step-to-source [db old-step-id new-step-id]
  (update-in db [:histories (:active-history db) :steps old-step-id] assoc :notify new-step-id))

(defn create-step [db id new-step]
  (update-in db [:histories (:active-history db) :steps] assoc id new-step))

; TODO - Hardcoded for testing, make this dynamic
(re-frame/register-handler
 :create-next-step
 trim-v
 (fn [db [tool-name]]
   (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
         source (:source last-emitted)
         data (:data last-emitted)
         uuid (keyword (rid))]
     (link-new-step-to-source (create-step db  uuid {:tool        "runtemplate"
                                                     :uuid        uuid
                                                     :title       "List Shower"
                                                     :description "View contents."
                                                     :has nil
                                                     :settled     true
                                                     :state       []})
                              (:step source)
                              uuid))))
