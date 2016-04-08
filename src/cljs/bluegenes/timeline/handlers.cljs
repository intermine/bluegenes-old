(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v]]
            [bluegenes.db :as db]
            [secretary.core :as secretary]
            [schema.core :as s]
            [bluegenes.schemas :as schemas]
            [bluegenes.utils :as utils])
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

(re-frame/register-handler
  :append-state
  trim-v
  (fn [db [step-id data]]
      (update-in db [:histories (:active-history db) :steps step-id :state] conj data)))

(re-frame/register-handler
  :replace-state
  trim-v
  (fn [db [step-id data]]
      (assoc-in db [:histories (:active-history db) :steps step-id :state] [data])))

(re-frame/register-handler
 :is-loading
 trim-v
 (fn [db [step-id data]]
   (assoc-in db [:histories (:active-history db) :steps step-id :loading?] data)))

(defn link-new-step-to-source [db old-step-id new-step-id]
  (update-in db [:histories (:active-history db) :steps old-step-id] assoc :notify new-step-id))

(defn create-step [db id new-step]
  (update-in db [:histories (:active-history db) :steps] assoc id new-step))

(defn clear-available-data
  "Clear the history of available data."
  [db]
  (assoc-in db [:histories (:active-history db) :available-data] nil))

(re-frame/register-handler
 :create-next-step
 trim-v
 (fn [db [tool-name]]
   (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
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



(defn truncate-view
  "Trims the :structure vector of a history to the current id."
  [db step-id]
  (let [structure (get-in db [:histories (:active-history db) :structure])]
    (assoc-in db [:histories (:active-history db) :structure]
              (utils/truncate-vector-to-value structure step-id) )))

(defn update-self [db data step-id]
  (.log js/console (clj->js [:histories
                 (:active-history db)
                 :steps
                 step-id]))
  (update-in db [:histories
                 (:active-history db)
                 :steps
                 step-id]
             assoc :produced data))

(defn spawn-shortcut [db data subscribed-to-step-id]
  (if (contains? data :shortcut)
    (let [uuid (keyword (rid))]
      (->
       (truncate-view db subscribed-to-step-id)
       (create-step uuid {:tool (:shortcut data)
                          :_id uuid
                          :scroll-to? true
                          :state []
                          :subscribe [subscribed-to-step-id]})
       (update-in [:histories (:active-history db) :structure] conj uuid)))
    db))

(re-frame/register-handler
 :has-something
 trim-v
 (fn [db [step-id data]]
   (s/validate schemas/Payload data)
   (-> db
       (update-self data step-id)
       (spawn-shortcut data step-id))))


(defn stamp-step
  "Stamps a step with a 'produced' attribute that stores the data consumed by
  next steps. This key becomes the input to other tools that subscribe
  to this step."
  [db source]
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
   :start-new-history
   trim-v
   (fn [db [tool data]]
    "Start a new history in app db."
    (let [new-step-id (rid) new-history-id (rid)]
      (aset js/window "location" "href" (str "/#timeline/" new-history-id))
      (-> db (assoc :active-history (keyword new-history-id))
          (create-step (keyword new-step-id)
            {:_id (keyword new-step-id)
             :state [data]
             :tool (:name tool)
             })
          (update-in [:histories (keyword new-history-id)] merge
                     {:slug new-history-id
                      :structure [(keyword new-step-id)]
                      :description (:name tool)
                      :name (:name tool)}))
      )))
