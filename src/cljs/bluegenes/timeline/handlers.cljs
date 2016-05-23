(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v enrich]]
            [bluegenes.db :as db]
            [secretary.core :as secretary]
            [schema.core :as s]
            [bluegenes.schemas :as schemas]
            [bluegenes.utils :as utils]
            [bluegenes.utils.imcljs :as im]
            [bluegenes.api :as api]
            [com.rpl.specter :as specter]
            [cljs.core.async :refer [put! chan <! >! timeout close!]])
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
                                                                                                   :step    step-id}))))))

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
  :update-state
  trim-v
  (fn [db [step-id f]]
    (update-in db [:histories (:active-history db) :steps step-id :state
                   (count (get-in db [:histories (:active-history db) :steps step-id :state]))] f
               )))

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
                                                                           :tool        tool-name
                                                                           :uuid        uuid
                                                                           :title       "No title"
                                                                           :description "No contents."
                                                                           :has         nil
                                                                           :input       last-emitted
                                                                           :settled     true
                                                                           :state       []})
                                                     (:step source)
                                                     uuid)))))



(defn truncate-view
  "Trims the :structure vector of a history to the current id."
  [db step-id]
  (let [structure (get-in db [:histories (:active-history db) :structure])]
    (assoc-in db [:histories (:active-history db) :structure]
              (utils/truncate-vector-to-value structure step-id))))

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
        (create-step uuid {:tool       (:shortcut data)
                           :_id        uuid
                           :scroll-to? true
                           :state      []
                           :subscribe  [subscribed-to-step-id]})
        (update-in [:histories (:active-history db) :structure] conj uuid)))
    db))

;(doall (map
;         #(re-frame/dispatch [:handle-parse-produced %])
;         (-> data :data :payload :select)))

(re-frame/register-handler
  :handle-parse-query
  trim-v
  (fn [db [step-id data]]
    ;(println "handle parse query called" data)
    (update-in db [:histories (:active-history db) :steps step-id :saver]
               (fnil (fn [pieces]
                       (conj pieces data)) []))))

(re-frame/register-handler
  :clear-parse-produced
  trim-v
  (fn [db [step-id]]
    ;(println "CLEAR PARSE PRODUCED")
    (assoc-in db [:histories (:active-history db) :steps step-id :saver] [])))


(re-frame/register-handler
  :handle-parse-ids
  trim-v
  (fn [db [step-id data]]
    (println "handling ids" data)
    (assoc-in db [:histories (:active-history db) :steps step-id :saver] [data])))




(defn parse-produced [db data step-id]
  (re-frame/dispatch [:clear-parse-produced step-id])
  (println "parsing produced")
  (cond
    (= "query" (-> data :data :format))
    (let [model (-> db :cache :models :flymine)
          deconstructed-query (im/deconstruct-query (-> db :cache :models :flymine)
                                                    (-> data :data :payload))]
      (println "deconstructed query" deconstructed-query)
      (assoc-in db [:histories (:active-history db) :steps step-id :extra] deconstructed-query)

      ;(go-loop [paths (seq deconstructed-query)]
      ;         (let [[path query] (first paths)]
      ;           (let [dn    (<! (im/get-display-name {:root "www.flymine.org/query"} path))
      ;                 count (<! (im/query-count {:root "www.flymine.org/query"} query))]
      ;             (re-frame/dispatch [:handle-parse-query
      ;                                 step-id
      ;                                 {:display-name dn
      ;                                  :query query
      ;                                  :service :flymine
      ;                                  :count count}])))
      ;         (if (not-empty (rest paths))
      ;           (recur (rest paths))))

      )

    (= "ids" (-> data :data :format))
    (do
      (re-frame/dispatch [:handle-parse-ids step-id data])
      db)
    :else db))

(defn enricher [db [_ step-id data]]
  ;(println "ENRICHER FIRING" step-id)
  ;(println "sees SAVER" (get-in db [:histories (:active-history db) :steps step-id :saver]))
  (assoc-in db [:histories (:active-history db) :steps step-id :saver] []))

(re-frame/register-handler
  :has-something
  (enrich enricher)
  (fn [db [_ step-id data]]
    ;(println "has-something" step-id data)
    (s/validate schemas/Payload data)
    (-> db
        (update-self data step-id)
        (parse-produced data step-id)
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
             assoc :produced {:data    (:data source)
                              :service (:service source)}))


(re-frame/register-handler
  :add-step
  trim-v
  (fn [db [tool-name state]]
    (.log js/console "add-step" tool-name state)
    ;(println "active history" (:active-history db))
    (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
          uuid (keyword (rid))]
      ;(println "last emitted" {:tool tool-name
      ;                         :_id uuid
      ;                         :state [state]
      ;                         :subscribe [(last (get-in db [:histories
      ;                                                       (:active-history db)
      ;                                                       :structure]))]})
      (-> db
          (update-in [:histories (:active-history db) :steps]
                     (fn [steps] (assoc steps uuid {:tool      tool-name
                                                    :_id       uuid
                                                    :state     [state]
                                                    :subscribe [(last (get-in db [:histories
                                                                                  (:active-history db)
                                                                                  :structure]))]})))
          (update-in [:histories (:active-history db) :structure]
                     (fn [structure] (conj structure uuid))))
      ;(-> db
      ;(create-step uuid {:tool tool-name
      ;                   :_id uuid
      ;                   :state [(clj->js state)]
      ;                   :subscribe [(last (get-in db [:histories
      ;                                                 (:active-history db)
      ;                                                 :structure]))]})
      ;(stamp-step last-emitted)
      ;(clear-available-data)
      ;)
      ;  (clear-data ((create-step db uuid {:tool tool-name
      ;                        :_id uuid
      ;                        :state []
      ;                        :subscribe [(:step source)]})))
      ;  (.log js/console "source" (clj->js source))
      ;  (.log js/console "data" (clj->js data))
      )))

(re-frame/register-handler
  :add-step-old
  trim-v
  (fn [db [tool-name state]]
    (.log js/console "add-step" tool-name state)
    (let [last-emitted (get-in db [:histories (:active-history db) :available-data])
          uuid (keyword (rid))]
      (println "last emitted" last-emitted)
      (-> db
          (create-step uuid {:tool      tool-name
                             :_id       uuid
                             :state     []
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
                       {:_id   (keyword new-step-id)
                        :state [data]
                        :tool  (:name tool)
                        })
          (update-in [:histories (keyword new-history-id)] merge
                     {:slug        new-history-id
                      :structure   [(keyword new-step-id)]
                      :description (:name tool)
                      :name        (:name tool)})))))

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
  :save-research-old
  trim-v
  (fn [db [id]]
    (let [steps (get-in db [:histories (:active-history db) :steps])
          uuid (keyword (rid))
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

(defn payload-is-query? [payload]
  (= "query" (:format (:data payload))))

(re-frame/register-handler
  :update-research-count
  (fn [db [_ id c]]
    (assoc-in db [:histories (:active-history db) :saved-research id :count] c)))

(re-frame/register-handler
  :save-research
  trim-v
  (fn [db [id data-to-save]]
    (println "saving researh" id data-to-save)
    (let [steps (get-in db [:histories (:active-history db) :steps])
          uuid (keyword (rid))
          update-path [:histories (:active-history db) :saved-research uuid]
          pruned-steps (steps-back-to-beginning steps id)
          key-map (generate-key-map pruned-steps)
          new-structure (apply-new-ids-to-structure
                          (get-in db [:histories (:active-history db) :structure])
                          id
                          key-map)]
      (if (payload-is-query? data-to-save)
        (go
          (println "saving query" (:payload (:data data-to-save)))
          (let [c (<! (im/query-count
                        {:root "www.flymine.org/query/service"}
                        (:payload (:data data-to-save))))]
            (println "c" c)
            (re-frame/dispatch [:update-research-count uuid c]))))
      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :structure new-structure
                 :editing true
                 :when (.now js/Date)
                 :payload data-to-save
                 :steps (-> steps
                            (steps-back-to-beginning id)
                            (apply-new-ids-to-steps key-map))))))



(def newsearch {:x1 {:_id   :x1
                     :tool  "dashboard"
                     :state [{:active 0
                              :tools  [{:tool  "smartidresolver"
                                        :state [{:example "one"}]}
                                       {:tool  "chooselist"
                                        :state [{:example "two"}]}]}]}})

(re-frame/register-handler
  :new-search
  trim-v
  (fn [db]
    (println "Creating a new search")
    (let [id (keyword rid)]
      (update-in db [:histories (:active-history db)]
                 (fn [x]
                   (println "GOT X" x)
                   (assoc x :steps newsearch
                            :structure [:x1]))))))

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
                                          :structure]))))))







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/register-handler
  :run-step trim-v
  (fn [db [location input]]
    (println "running step")
    (let [node (get-in db location)
          run-fn (-> bluegenes.tools
                     (aget (:tool node))
                     (aget "core")
                     (aget "run"))]
      (run-fn input
              (:state node)
              (:cache node)
              {:has-something (partial api/has-something location)
               :save-state (partial api/save-state location)
               :save-cache (partial api/save-cache location)}))
    db))

(defn subscribers [db location]
  (let [id (last location)]
    (specter/select [(butlast location) specter/ALL specter/LAST
                     #(= id (:subscribe-to %))
                     :_id] db)))

(re-frame/register-handler
  :has-something trim-v
  (fn [db [location data]]
    (doall
      (map (fn [subscriber]
             (re-frame/dispatch [:run-step
                                 (conj (vec (butlast location)) subscriber)
                                 data]))
           (subscribers db location)))
    (assoc-in db (conj location :output) data)))

(re-frame/register-handler
  :save-state trim-v
  (fn [db [location data]]
    (assoc-in db (conj location :state) data)))

(re-frame/register-handler
  :save-cache trim-v
  (fn [db [location data]]
    (assoc-in db (conj location :cache) data)))
