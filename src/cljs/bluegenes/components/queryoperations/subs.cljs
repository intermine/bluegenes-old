(ns bluegenes.components.queryoperations.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]))

(re-frame/register-sub
  :qop-1
  (fn [db [_]]
    (let [qop (reaction (get-in @db [:projects (:active-project @db)
                                     :networks (:active-network @db)
                                     :query-operations :1]))]
      (reaction (assoc (get-in @db [:projects (:active-project @db)
                              :saved-data (:id @qop)])
                  :keep (:keep @qop))))))

(re-frame/register-sub
  :qop-2
  (fn [db [_]]
    (let [qop (reaction (get-in @db [:projects (:active-project @db)
                                     :networks (:active-network @db)
                                     :query-operations :2]))]
      (reaction (assoc (get-in @db [:projects (:active-project @db)
                              :saved-data (:id @qop)])
                  :keep (:keep @qop))))))

(re-frame/register-sub
  :qop-op
  (fn [db [_]]
    (reaction (get-in @db [:projects (:active-project @db)
                           :networks (:active-network @db)
                           :query-operations :operation]))))


