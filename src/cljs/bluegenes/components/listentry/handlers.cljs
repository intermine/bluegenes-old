(ns bluegenes.components.listentry.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]
            [bluegenes.db :as db]))

(re-frame/register-handler
 :list-entry-has-something
 (fn [db [_ id & args]]
   (assoc-in db [:entry-points :lists] args)))
