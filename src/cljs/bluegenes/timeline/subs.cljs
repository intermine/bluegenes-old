(ns bluegenes.timeline.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]))

(re-frame/register-sub
  :saved-research
  (fn [db _]
    (reaction (get-in @db [:projects (:active-project @db) :saved-data]))))

(re-frame/register-sub
 :steps
 (fn [db [_ testvalue]]
   (reaction (get-in @db [:projects (:active-project @db)
                          :networks (:active-network @db) :nodes]))))

(re-frame/register-sub
  :active-data
  (fn [db [_ testvalue]]
    (reaction (get-in @db [:projects (:active-project @db)
                           :saved-data (:active-data @db)]))))

(re-frame/register-sub
 :step-path
 (fn [db [_ testvalue]]
   (reaction (get-in @db [:projects (:active-project @db)
                          :networks (:active-network @db) :view]))))

(re-frame/register-sub
  :networks
  (fn [db [_]]
    (reaction (get-in @db [:projects (:active-project @db) :networks]))))

(re-frame/register-sub
 :to-step
 (fn [db [_ step-id]]
   (reaction (get-in @db [:histories (:active-history @db) :steps step-id]))))

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