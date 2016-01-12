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
  (fn [db [tool data]]
    (let [idx (get-idx (:uuid tool) (get-in db [:histories (:active-history db) :steps]))]
      (update-in db [:histories (:active-history db) :steps idx :state] conj data))))

(re-frame/register-handler
  :has-something
  trim-v
  (fn [db [tool data & settle]]
    (let [idx (get-idx (:uuid tool) (get-in db [:histories (:active-history db) :steps]))]
      (re-frame/dispatch [:pass-input data (inc idx)])
      (update-in db [:histories (:active-history db) :steps idx] assoc :has data))))
