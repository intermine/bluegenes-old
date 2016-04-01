(ns bluegenes.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]
            [bluegenes.db :as db]
            [bluegenes.timeline.handlers]
            [intermine.imjs :as imjs]
            [cljs-http.client :as http]
            [cljs.core.async :refer [chan <!]]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
  :set-active-history
  (fn [db [_ active-panel & args]]
    (assoc db :active-history (:id (first args)))))

(re-frame/register-handler
 :set-search-term
 (fn [db [_ search-term & args]]
   (assoc db :search-term search-term)))


(re-frame/register-handler
 :set-active-panel
 ;"Clear active history and move to specified panel. Clearing history is
 ;important for the homepage wadgets so they don't get associated with the wrong
 ;history."
 (fn [db [_ active-panel & args]]
   (assoc db :active-panel active-panel :active-history nil)))

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

(re-frame/register-handler
 :set-user
 trim-v
 (fn [db [token email name picture]]
   (assoc-in db [:whoami]
             {:name name
              :email email
              :picture picture
              :token token
              :authenticated true})))

(re-frame/register-handler
 :process-histories
 trim-v
 (fn [db response]
   "Merge histories from the server into the existing map of histories."
   (update-in db [:histories]
              merge
              (reduce
               (fn [history-map next-history]
                 (assoc history-map
                        (keyword (:_id next-history))
                        next-history))
               {}
               (first response)))))

(re-frame/register-handler
 :load-histories
 trim-v
 (fn [db]
   "Get histories from the server."
   (go (let [res (<! (http/get "/api/history"
                               {:with-credentials? false
                                :keywordize-keys? true}))]
         (re-frame/dispatch [:process-histories res])))
   db))
