(ns bluegenes.timeline.handlers
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v enrich]]
            [bluegenes.db :as db]
            [secretary.core :as secretary]
            [secretary.core :as secretary]
            [schema.core :as s]
            [bluegenes.schemas :as schemas]
            [bluegenes.utils :as utils]
            [bluegenes.utils.imcljs :as im]
            [bluegenes.api :as api]
            [com.rpl.specter :as specter]
            [cuerdas.core :as cue]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report
                                logf tracef debugf infof warnf errorf fatalf reportf
                                spy get-env log-env)])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(enable-console-print!)

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
          source       (:source last-emitted)
          data         (:data last-emitted)
          uuid         (keyword (rid))]
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
    (let [model               (-> db :cache :models :flymine)
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
  :add-step-temp
  trim-v
  (fn [db [tool-name state]]
    (.log js/console "add-step" tool-name state)
    ;(println "active history" (:active-history db))
    (let [last-emitted (last (get-in db [:networks (:active-history db) :view]))
          uuid         (keyword (rid))]
      (re-frame/dispatch [:run-step
                          [:networks (:active-history db) :nodes uuid] [:state :input]])
      (->
        db
        (assoc-in [:networks (:active-history db) :nodes uuid]
                  {:tool         tool-name
                   :state        nil
                   :_id          uuid
                   :subscribe-to last-emitted})
        (update-in [:networks (:active-history db) :view] conj uuid)))))

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

(re-frame/register-handler
  :new-network trim-v
  (fn [db]
    (let [active-project (:active-project db)
          id             (keyword (rid))]
      (assoc-in db [:projects active-project :networks id]
                {:_id   id
                 :slug  "madeup"
                 :label "MADE UP Network"
                 :view  [:node1]
                 :nodes {:node1 {:_id          :node1
                                 :tool         "chooselist"
                                 :subscribe-to nil}}}))))

(defn steps-back-to-beginning
  "Build a map of only this step and the steps required to replay
  this step from the root of the workflow. This is useful for forking steps,
  copying workflows, and trimming siblings and childrens."
  [steps end]
  (loop [m       {}
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

(defn trimmed-structure
  "Give a vector structure of keywords, a keyword to stop at, and a map
  of old ids to new ids, prune the structure vector and replace ids accordingly."
  [structure end]
  (take (inc (index-of end structure)) structure))

; This SERIOUSLY needs to be refactored. This should be easy. Need more coffee.
(re-frame/register-handler
  :save-research-old
  trim-v
  (fn [db [id]]
    (let [steps         (get-in db [:histories (:active-history db) :steps])
          uuid          (keyword (rid))
          update-path   [:histories (:active-history db) :saved-research uuid]
          pruned-steps  (steps-back-to-beginning steps id)
          key-map       (generate-key-map pruned-steps)
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
    (let [steps         (get-in db [:projects (:active-project db)
                                    :networks (:active-network db) :nodes])
          uuid          (keyword (rid))
          update-path   [:projects (:active-project db) :saved-data uuid]
          new-structure (trimmed-structure
                          (get-in db [:projects (:active-project db)
                                      :networks (:active-network db) :view])
                          id)]


      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 ;:view new-structure
                 :editing true
                 :when (.now js/Date)
                 ;:nodes (-> steps (steps-back-to-beginning id))
                 :payload data-to-save))))



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
    (update-in db [:projects (:active-project db)
                   :saved-data id]
               assoc
               :label value
               :slug (cue/slugify value)
               :editing false)))

(re-frame/register-handler
  :load-search
  trim-v
  (fn [db [id value]]
    (println "LOADING RESEARCH ID" id)
    (assoc db :active-network id)))

(re-frame/register-handler
  :load-research
  trim-v
  (fn [db [id value]]
    (println "LOADING RESEARCH ID" id)
    (assoc db :active-network id)))







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn subscribers
  "Returns a vector of ids that are subscribed to the current location."
  [db location]
  (let [id (last location)]
    (specter/select [(butlast location) specter/ALL specter/LAST
                     #(= id (:subscribe-to %))
                     :_id] db)))

(defn get-changes-old
  "Returns a new map of where the value of the key in the
  new map is different from the value of the key in the old map."
  [old-m new-m]
  (reduce (fn [total [k v]]
            (if (not= (k old-m) (k new-m))
              (assoc total k v)
              total)) {} new-m))

(defn get-changes
  "Walks map m2 depth first keeping only key value pairs that are difer between map m1 and m2."
  [m1 m2]
  (reduce (fn [total [k v]]
            (if (= (get m1 k) (get m2 k))
              total
              (assoc total k (if (map? v)
                               (get-changes (get m1 k) (get m2 k))
                               v)))) {} m2))

(re-frame/register-handler
  :run-step trim-v
  (fn [db [location diffmap]]
    (let [node   (get-in db location)
          run-fn (-> bluegenes.tools (aget (:tool node)) (aget "core") (aget "run"))]

      ; Run input through the filter


      (let [node (cond-> node
                         (:filter node) (assoc-in [:input :data :payload :select] [(:filter node)]))]
        (run-fn
          node                                              ;Snapshot
          (if (nil? diffmap) node diffmap)                  ;Difference in data
          {:has-something (partial api/has-something location) ;API
           :save-state    (partial api/save-state location)
           :update-state    (partial api/update-state location)
           :save-cache    (partial api/save-cache location)
           :update-cache (partial api/update-cache location)}
          (get-in db [:cache])                              ;Global Cache
          )))
    db))

(re-frame/register-handler
  :calculate-export trim-v
  (fn [db [location output]]
    ;(if (= "query" (:format (:data output)))
    ;  (let [m (get-in db [:cache :models :flymine])]
    ;    (.log js/console "HAS DECONSTRUCTED" (im/deconstruct-query-by-class m (:payload (:data output))))))
    (cond (= "query_ignorefornow" (:format (:data output)))
          (let [m (get-in db [:cache :models :flymine])]
            (.log js/console "HAS DECONSTRUCTED" (im/deconstruct-query m (:payload (:data output))))
            (assoc-in db (conj location :export) (im/deconstruct-query m (:payload (:data output)))))
          :else
          (assoc-in db (conj location :export) output))))

(re-frame/register-handler
  :update-node trim-v
  (fn [db [location update-fn]]
    ;(println "update node given location" location)
    (let [snapshot (get-in db location)
          updated  (update-fn snapshot)
          diffed   (get-changes snapshot updated)]

      ; If something has changed then re-run the tool with
      ; the new data (only the difference)
      (if-not (empty? diffed)
        (do
          (re-frame/dispatch ^:flush-dom [:run-step location diffed])

          ; If the difference map contains an :output key then we must
          ; feed it as an input to each subscriber and re-run them.
          (if (contains? diffed :output)

            (do
              ; Deconstruct the output to individual parts for exporting
              ; (saving to the drawer)
              (re-frame/dispatch [:calculate-export location (:output updated)])

              ; Give all subscribers their new input and run them.
              (mapv
                (fn [subscriber]
                  (let [decon (doall (im/deconstruct-query-by-class
                                       (get-in db [:cache :models :flymine])
                                       (:payload (:data (:output updated)))))]

                    (re-frame/dispatch [:update-node (conj (vec (butlast location)) subscriber)
                                        #(assoc % :input (:output updated) :decon decon)])))
                (subscribers db location))))))

      (assoc-in db location updated))))

(re-frame/register-handler
  :add-step
  trim-v
  (fn [db [tool-name state]]
    (let [project         (get-in db [:active-project])
          network         (get-in db [:active-network])
          panel           (first (get-in db [:active-panel]))
          last-emitted    (last (get-in db [:projects project :networks network :view]))
          uuid            (keyword (rid))
          previous-output (get-in db [:projects project :networks network :nodes last-emitted :output])
          node            {:tool         tool-name
                           :state        state
                           :_id          uuid
                           :input        previous-output
                           :decon        (im/deconstruct-query-by-class
                                           (get-in db [:cache :models :flymine])
                                           (-> previous-output :data :payload))
                           :subscribe-to last-emitted}]

      (case panel
        :saved-data-panel
        (let [id (keyword (rid))]
          (let [current-data (get-in db [:projects (:active-project db) :saved-data (:active-data db)])]
            (re-frame/dispatch ^:flush-dom [:run-step [:projects project :networks id :nodes uuid] :node1])
            (secretary/dispatch! (str "/timeline/project1/" (str id)))
            (-> db (assoc-in [:projects project :networks id]
                             {:_id   id
                              :slug  (str id)
                              :label "New Search"
                              :view  [:node1 uuid]
                              :nodes {:node1 (assoc current-data :_id :node1
                                                                 :state (:payload current-data)
                                                                 :tool "viewtable")
                                      uuid   (assoc node :subscribe-to :node1)}}))))
        :timeline-panel (do
                          (re-frame/dispatch ^:flush-dom [:run-step [:projects project :networks network :nodes uuid] node])
                          (->
                            db
                            (assoc-in [:projects project :networks network :nodes uuid] node)
                            (update-in [:projects project :networks network :view] conj uuid)))))))


(re-frame/register-handler
  :set-input-filter trim-v
  (fn [db [id path]]
    (let [location [:projects (:active-project db) :networks (:active-network db) :nodes id]
          snapshot (get-in db location)
          updated  (assoc snapshot :filter path)
          diffmap  (get-changes snapshot updated)]

      (re-frame/dispatch ^:flush-dom [:run-step location {:input (:input snapshot)}])
      (assoc-in db location updated))))