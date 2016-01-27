(ns bluegenes.handlers
    (:require [re-frame.core :as re-frame :refer [trim-v]]
              [bluegenes.db :as db]
              [bluegenes.timeline.handlers]
              [intermine.imjs :as imjs]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
  :set-active-history
  (fn [db [_ active-panel & args]]
    (assoc db :active-history (:id (first args)))))


(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel & args]]
   (assoc db :active-panel active-panel)))

(re-frame/register-handler
 :set-timeline-panel trim-v
 (fn [db [active-panel & [slug]]]
   ;Look up the UUID of the history based on the slug (friendly) name
   ;TODO - if the slug doesn't exist then check for the UUID directly
   (let [uuid (first (keep #(when (= (:slug (val %)) slug) (key %)) (:histories db)))]
     (assoc db :active-panel active-panel :active-history uuid))))

(re-frame/register-handler
 :activate-dimmer
 (fn [db [_ message]]
   (.log js/console "activate dimmer handler" message)
   (assoc db :dimmer {:active true :message message})))

(re-frame/register-handler
 :deactivate-dimmer
 (fn [db [_]]
   (assoc db :dimmer {:active false :message nil})))
