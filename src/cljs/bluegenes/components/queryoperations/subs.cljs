(ns bluegenes.components.queryoperations.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]))

(re-frame/register-sub
  :qop-1
  (fn [db [_]]
    (reaction (-> (get-in @db [:projects (:active-project @db) :query-operations :targets :1])
                  (assoc :keep (get-in @db [:projects (:active-project @db)
                                            :query-operations :states :1 :keep]))))))

(re-frame/register-sub
  :qop-2
  (fn [db [_]]
    (reaction (-> (get-in @db [:projects (:active-project @db) :query-operations :targets :2])
                  (assoc :keep (get-in @db [:projects (:active-project @db)
                                            :query-operations :states :2 :keep]))))))

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

