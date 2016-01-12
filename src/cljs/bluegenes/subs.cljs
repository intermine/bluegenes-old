(ns bluegenes.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]
              [bluegenes.timeline.subs]))

(re-frame/register-sub
 :name
 (fn [db]
   (reaction (:name @db))))

(re-frame/register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(re-frame/register-sub
  :dimmer
  (fn [db _]
    (reaction (:dimmer @db))))

(re-frame/register-sub
  :app-state
  (fn [db]
    (reaction @db)))

(re-frame/register-sub
  :all-histories
  (fn [db]
    (reaction (:histories @db))))