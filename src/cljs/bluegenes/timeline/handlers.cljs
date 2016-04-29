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
                      :name (:name tool)})))))

(defn steps-back-to-beginning
  "Build a map of only this step and the steps required to replay
  this step from the root of the workflow. This is useful for forking steps,
  copying workflows, and trimming siblings and childrens."
  [steps end]
  (loop [m {}
         step-id end]
    (let [current-step (-> steps step-id)]
      (if (contains? current-step :subscribe)
        (recur (assoc m (:_id current-step) current-step)
               (first (:subscribe current-step)))
        (assoc m (:_id current-step) current-step)))))


(defn update-children [m km]
  (if (contains? m :subscribe)
    (update m :subscribe
            (fn [subscribed-steps]
              (map (fn [s]
                     (s km)) subscribed-steps)))
    m))

(defn apply-new-ids-to-steps
  "Given a map of steps in a workflow, generates new ids for each step
  and updates the :subscribe values accordingly."
  [m km]
  (let [key-map km]
    (reduce (fn [new-map [k v]]
              (assoc new-map (k key-map)
                             (-> v
                                 (assoc :_id (k key-map))
                                 (update-children key-map)))) {} m)))

(defn generate-key-map
  "Generate a map of existing keys to new UUIDs.
  {:old-id1 :some-new-uuid
   :old-id2 :some-other-new-uuid}"
  [m]
  (reduce (fn [key-map [k v]]
            (assoc key-map k (keyword (rid)))) {} m))

(defn index-of
  "Find the index of the first occurence of an element in a collection."
  [e coll] (first (keep-indexed #(if (= e %2) %1) coll)))

(defn apply-new-ids-to-structure
  "Give a vector structure of keywords, a keyword to stop at, and a map
  of old ids to new ids, prune the structure vector and replace ids accordingly."
  [structure end key-map]
  (mapv (fn [x]
          (x key-map)) (take (inc (index-of end structure)) structure)))

; This SERIOUSLY needs to be refactored. This should be easy. Need more coffee.
(re-frame/register-handler
  :save-research
  trim-v
  (fn [db [id]]
    (let [steps       (get-in db [:histories (:active-history db) :steps])
          uuid        (keyword (rid))
          update-path [:histories (:active-history db) :saved-research uuid]
          pruned-steps (steps-back-to-beginning steps id)
          key-map (generate-key-map pruned-steps)
          new-structure (apply-new-ids-to-structure
                          (get-in db [:histories (:active-history db) :structure])
                          id
                          key-map)]
      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :structure new-structure
                 :editing true
                 :saved (.now js/Date)
                 :steps (-> steps
                            (steps-back-to-beginning id)
                            (apply-new-ids-to-steps key-map))))))


(re-frame/register-handler
  :new-search
  trim-v
  (fn [db]
    (println "Creating a new search")
    db))

(re-frame/register-handler
  :relabel-research
  trim-v
  (fn [db [id value]]
    (update-in db [:histories (:active-history db) :saved-research id]
               assoc
               :label value
               :editing false)))

(re-frame/register-handler
  :load-research
  trim-v
  (fn [db [id value]]
    (println "assocign research")
    (update-in db [:histories (:active-history db)]
               (fn [history]
                 (assoc history
                   :steps (get-in db [:histories
                                                   (:active-history db)
                                                   :saved-research
                                                   id
                                                   :steps])
                   :structure (get-in db [:histories
                                          (:active-history db)
                                          :saved-research
                                          id
                                          :structure]) )))))