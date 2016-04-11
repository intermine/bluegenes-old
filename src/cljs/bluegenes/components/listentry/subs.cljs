(ns bluegenes.components.listentry.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
  :list-entry-data
  (fn [db]
    (reaction (:lists (:entry-points @db)))))
