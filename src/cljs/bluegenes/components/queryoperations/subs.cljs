(ns bluegenes.components.queryoperations.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]))

(re-frame/register-sub
  :qop-1
  (fn [db [_]]
    (let [qops (reaction (get-in @db [:projects (:active-project @db) :query-operations]))]
      (reaction (assoc (get-in @db [:projects (:active-project @db)
                                    :saved-data (:id (:1 (:targets @qops)))])
                  :keep (:keep (:1 (:states @qops))))))))

(re-frame/register-sub
  :qop-2
  (fn [db [_]]
    (let [qops (reaction (get-in @db [:projects (:active-project @db) :query-operations]))]
      (reaction (assoc (get-in @db [:projects (:active-project @db)
                                    :saved-data (:id (:2 (:targets @qops)))])
                  :keep (:keep (:2 (:states @qops))))))))

(re-frame/register-sub
  :qop-op
  (fn [db [_]]
    (reaction (get-in @db [:projects (:active-project @db)
                           :query-operations :operation]))))

(re-frame/register-sub
  :qop-middle
  (fn [db [_]]
    (reaction (get-in @db [:projects (:active-project @db)
                           :query-operations :states :middle]))))

