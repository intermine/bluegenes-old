(ns bluegenes.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame :refer [trim-v]]
            [bluegenes.db :as db]
            [bluegenes.timeline.handlers]
            [bluegenes.components.listentry.handlers]
            [intermine.imjs :as imjs]
            [bluegenes.utils.imcljs :as im]
            [cljs-http.client :as http]
            [com.rpl.specter :as s]
            [cljs.core.async :refer [chan <!]]))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/register-handler
  :handle-bootstrap-template
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :templates mine-key] response)))



(re-frame/register-handler
  :handle-bootstrap-model
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :models mine-key] response)))

(re-frame/register-handler
  :set-active-history
  (fn [db [_ active-history & args]]
    (assoc-in db [:active-history] active-history)))

(re-frame/register-handler
  :set-active-network
  (fn [db [_ active-network & args]]
    (re-frame/dispatch [:run-step [:projects :project1 :networks active-network :nodes :node1]])
    (assoc-in db [:active-network] active-network)))


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
  (fn [db [active-panel & [project-slug network-slug]]]
    ;Look up the UUID of the history based on the slug (friendly) name
    ;TODO - if the slug doesn't exist then check for the UUID directly
    (println "***** SET timeline panel" project-slug network-slug)
    (let [uuid (first (keep #(when (= (:slug (val %)) project-slug) (key %)) (:projects db)))]

      (let [[project network] (vec (butlast (s/select-one [:projects s/ALL s/LAST
                                                          #(= project-slug (:slug %))
                                                          (s/collect-one :_id)
                                                          :networks s/ALL s/LAST
                                                          #(= network-slug (:slug %))
                                                          (s/collect-one :_id)]
                                                         db)))]

        (let [[x y] (s/select-one [:projects s/ALL s/LAST
                                   #(= "project1" (:slug %))
                                   (s/collect-one :_id)
                                   :networks s/ALL s/LAST
                                   #(= "network2" (:slug %))
                                   (s/collect-one :_id)
                                   butlast]
                                  db)]
          (println "X" x))
        (println "sees project" project)

        (assoc db :active-panel [active-panel project-slug network-slug]
                  :active-project project
                  :active-network network)))))

(re-frame/register-handler
  :set-saved-data-panel trim-v
  (fn [db [active-panel project-slug network-slug]]
    ;Look up the UUID of the history based on the slug (friendly) name
    ;TODO - if the slug doesn't exist then check for the UUID directly
    (println "***** SET SAVED DATA PANEL" project-slug network-slug)
    (let [uuid (first (keep #(when (= (:slug (val %)) project-slug) (key %)) (:projects db)))]

      (let [[project did] (vec (butlast (s/select-one [:projects s/ALL s/LAST
                                                           #(= project-slug (:slug %))
                                                           (s/collect-one :_id)
                                                           :saved-data s/ALL s/LAST
                                                           #(= network-slug (:slug %))
                                                           (s/collect-one :_id)]
                                                          db)))]
        (println "sees project" project did)
        (assoc db :active-panel [active-panel]
                  :active-project project
                  :active-data did)))))

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
              {:name          name
               :email         email
               :picture       picture
               :token         token
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
                                 :keywordize-keys?  true}))]
          (re-frame/dispatch [:process-histories res])))
    db))

(re-frame/register-handler
  :handle-bootstrap-template
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :templates mine-key] response)))

(re-frame/register-handler
  :handle-bootstrap-model
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :models mine-key] response)))

(re-frame/register-handler
  :handle-bootstrap-list
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :lists mine-key] response)))

(re-frame/register-handler
  :handle-bootstrap-summary-fields
  (fn [db [_ mine-key response]]
    (assoc-in db [:cache :summary-fields mine-key] response)))

(re-frame/register-handler
  :bootstrap-app
  (fn [db _]
    (println "Bootstrapping Appliation")
    (doall (map (fn [[mine-key mine-details]]
                  (go (re-frame/dispatch [:handle-bootstrap-template mine-key (<! (im/templates (:service mine-details)))]))
                  (go (re-frame/dispatch [:handle-bootstrap-list mine-key (<! (im/lists (:service mine-details)))]))
                  (go (re-frame/dispatch [:handle-bootstrap-model mine-key (<! (im/model (:service mine-details)))]))
                  (go (re-frame/dispatch [:handle-bootstrap-summary-fields mine-key (<! (im/all-summary-fields (:service mine-details)))])))
                (get-in db [:remote-mines])))
    db))
