(ns bluegenes.timeline.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]))

(re-frame/register-sub
  :saved-research
  (fn [db _]
    (let [s (reaction (get-in @db [:histories (:active-history @db) :saved-research]))]
      (reaction (into (sorted-map-by (fn [key1 key2]
                              ;(println "looking at key1" key1)
                              ;(println "looking at key2" key2)
                              (compare [(get-in @s [key2 :saved]) key2]
                                       [(get-in @s [key1 :saved]) key1])))
             @s)))))

(re-frame/register-sub
 :steps
 (fn [db [_ testvalue]]
   (reaction (get-in @db [:projects :project1 :networks :network1 :nodes]))))

(re-frame/register-sub
 :step-path
 (fn [db [_ testvalue]]
   (reaction (get-in @db [:projects :project1 :networks :network1 :view]))))

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