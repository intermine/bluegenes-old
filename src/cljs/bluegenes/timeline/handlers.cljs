(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v]]
            [bluegenes.db :as db]))

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
  (fn [db [tool data]]
    (let [idx (get-idx (:uuid tool) (get-in db [:histories (:active-history db) :steps]))]
      (assoc-in db [:histories (:active-history db) :steps idx :state] [data]))))

(re-frame/register-handler
  :has-something
  trim-v
  (fn [db [step-id data]]
      (update-in db [:histories (:active-history db)] assoc :available-data (assoc data :source {:history (:active-history db)
                                                                                                 :step step-id}))))


(re-frame/register-handler
 :create-next-step
 trim-v
 (fn [db [tool-name]]
   (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
         source (:source last-emitted)
         data (:data last-emitted)]
     (update-in db [:histories (:active-history db) :steps] assoc :banananana {:tool        "faketool"
                                                                               :uuid        "33333-4cdd-4536-8e91-bba0b17e4126"
                                                                               :title       "List Shower"
                                                                               :description "View contents."
                                                                               :input "TEST"
                                                                               :has nil
                                                                              ;  :notify :1dd2a806-d602-4fea-bb79-7f17915bc2c2
                                                                               :settled     true
                                                                               :state       []}))))
