(ns bluegenes.timeline.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
  :steps
  (fn [db _]
    (println "fetching steps")
    (reaction (get-in @db [:histories (:active-history @db) :steps]))))

(re-frame/register-sub
 :history
 (fn [db _]
   (reaction (get-in @db [:histories (:active-history @db)]))))

(re-frame/register-sub
  :mines
  (fn [db _]
    (reaction (get-in @db [:mines]))))


(re-frame/register-sub
  :settled-steps
  (fn [db _]
    (reaction (filter #(true? (:settled %)) (get-in @db [:current-steps])))))


(re-frame/register-sub
  :replay-timeline
  (fn [db _]
    (reaction (filter #(true? (:settled %)) (get-in @db [:current-steps])))))

(re-frame/register-sub
 :available-data
 (fn [db _]
   (reaction (get-in @db [:histories (:active-history @db) :available-data]))))
