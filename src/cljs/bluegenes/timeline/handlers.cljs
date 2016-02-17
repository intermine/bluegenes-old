(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v]]
            [bluegenes.db :as db])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(enable-console-print!)

(re-frame/register-handler
 :has-something-old
 trim-v
 (fn [db [step-id data]]
   (let [notify (get-in db [:histories (:active-history db) :steps step-id :notify])]
     ; If our tool has other tools to notify then push the data direclty to them
     (if-not (nil? notify)
       (update-in db [:histories (:active-history db) :steps notify] assoc :input data)
       (update-in db [:histories (:active-history db)] assoc :available-data (assoc data :source {:history (:active-history db)
        :step step-id}))))))

(defn rid [] (str (make-random-uuid)))

(defn new-history [db]
  "Generates a new history and resturns this history's id"
  (.log js/console "We're making a new history, folks")
  (let [new-history-id (rid) new-step-id (rid)]
    (update-in db [:histories new-history-id] {
      :steps {(keyword new-step-id) "new history"
      :_id new-step-id}})
    new-history-id))

(defn get-active-history [db]
  "returns either the current active history's ID (if there is one),
   or makes a new one and returns its ID"
  (if (some? (:active-history db))
    (:active-history db)
    (new-history db)))

(re-frame/register-handler
  :append-state
  trim-v
  (fn [db [step-id data]]
      (update-in db [:histories (get-active-history db) :steps step-id :state] conj data)))

(re-frame/register-handler
  :replace-state
  trim-v
  (fn [db [step-id data]]
      (assoc-in db [:histories (get-active-history db) :steps step-id :state] [data])))

(defn link-new-step-to-source [db old-step-id new-step-id]
  (update-in db [:histories (get-active-history db) :steps old-step-id] assoc :notify new-step-id))

(defn create-step [db id new-step]
  (update-in db [:histories (get-active-history db) :steps] assoc id new-step))

(defn clear-available-data
  "Clear the history of available data."
  [db]
  (assoc-in db [:histories (:active-history db) :available-data] nil))

(re-frame/register-handler
 :create-next-step
 trim-v
 (fn [db [tool-name]]
   (let [last-emitted (get-in db [:histories (get-active-history db) :available-data])
         source (:source last-emitted)
         data (:data last-emitted)
         uuid (keyword (rid))]
     (clear-available-data (link-new-step-to-source (create-step db uuid {
        :tool tool-name
        :uuid uuid
        :title "No title"
        :description "No contents."
        :has nil
        :input last-emitted
        :settled true
        :state []})
        (:step source)
        uuid)))))

(re-frame/register-handler
 :has-something
 trim-v
 (fn [db [step-id data]]
   (if (nil? (get-in db [:histories
                         (:active-history db)
                         :steps
                         step-id
                         :produced]))
     (update-in db [:histories (:active-history db)] assoc :available-data
                (assoc data :source {:history (:active-history db)
                                     :step step-id}))
     (update-in db [:histories
                    (:active-history db)
                    :steps
                    step-id]
                assoc :produced data))))


(defn stamp-step
  "Stamps a step with a 'produced' attribute that stores the data consumed by
  next steps. This key becomes the input to other tools that subscribe
  to this step."
  [db source]
  (.log js/console "got source" (clj->js source))
  (update-in db [:histories
                 (:active-history db)
                 :steps
                 (:step (:source source))]
             assoc :produced {:data (:data source)
                              :service (:service source)}))

(re-frame/register-handler
 :add-step
 trim-v
 (fn [db [tool-name]]
   (.log js/console "add-step" tool-name)
   (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
         uuid (keyword (rid))]
     (-> db
         (create-step uuid {:tool tool-name
                            :_id uuid
                            :state []
                            :subscribe [(:step (:source last-emitted))]})
         (stamp-step last-emitted)
         (clear-available-data))
    ;  (clear-data ((create-step db uuid {:tool tool-name
    ;                        :_id uuid
    ;                        :state []
    ;                        :subscribe [(:step source)]})))
    ;  (.log js/console "source" (clj->js source))
    ;  (.log js/console "data" (clj->js data))
     )))


 (re-frame/register-handler
   :start-history
   trim-v
   (fn [db [tool data]]
    "Start a new history in app db."
    (let [new-id (rid)]
      (.log js/console "dfsdfsdf sdf sdf sdf sdf" (clj->js data))
      (assoc-in db [:histories (keyword new-id)]
        {:steps {(keyword (rid)) data}})
        new-id)
    db))
