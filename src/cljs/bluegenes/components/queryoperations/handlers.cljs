(ns bluegenes.components.queryoperations.handlers
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [debug trim-v enrich register-handler]]
            [bluegenes.db :as db]
            [secretary.core :as secretary]
            [schema.core :as s]
            [bluegenes.schemas :as schemas]
            [bluegenes.utils :as utils]
            [bluegenes.utils.imcljs :as im]
            [bluegenes.api :as api]
            [com.rpl.specter :as specter]
            [cuerdas.core :as cue]
            [clojure.set :as set :refer [union intersection difference]]
            [cljs.core.async :as async :refer [put! chan <! >! timeout close!]]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report
                                logf tracef debugf infof warnf errorf fatalf reportf
                                spy get-env log-env)])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(defn rid [] (str (make-random-uuid)))


(def alphabet (into [] "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(defn convert-list-to-query [db item]
  (println "item" item)
  (println "converted" (get-in db [:remote-mines (:service item) :service]))
  (let [found-list (first (filter #(= (:id item) (:name %)) (get-in db [:cache :lists (:service item)])))
        service    (get-in db [:remote-mines (:service item) :service])]
    {:select "*"
     :from   (:type found-list)
     :where  [{:path  (:type found-list)
               :op    "IN"
               :value (:name found-list)}]}))

(re-frame/register-handler
  :set-qop trim-v
  (fn [db [position id type service]]
    (let [query (if (= :list type)
                  (convert-list-to-query db {:id id :service service})
                  (get-in db [:projects (:active-project db)
                              :saved-data id :payload :data :payload]))]
      (update-in db [:projects (:active-project db) :query-operations :targets (keyword (str position))]
                 assoc
                 :id id
                 :deconstructed (im/deconstruct-query-by-class (-> db :cache :models :flymine) query)
                 :type type
                 :service service))))

(re-frame/register-handler
  :update-qop-query trim-v
  (fn [db [position query]]
    (assoc-in db [:projects (:active-project db) :query-operations :targets (keyword (str position)) :query]
               query)
    ))




(re-frame/register-handler
  :toggle-qop trim-v
  (fn [db [position]]
    (println "toggling position" position)



    (let [location [:projects (:active-project db)
                    :query-operations :states (keyword (str position))
                    :keep]]

      (re-frame/dispatch [:determine-qop])

      (update-in db location not))))

(re-frame/register-handler
  :toggle-qop-middle trim-v
  (fn [db [position]]
    (println "toggling position" position)
    (let [location [:projects (:active-project db)
                    :query-operations :states :middle]]
      (re-frame/dispatch [:determine-qop])
      (update-in db location not))))


(re-frame/register-handler
  :set-qop-op trim-v
  (fn [db [value]]
    (assoc-in db [:projects (:active-project db)
                  :query-operations :operation] value)))


(defn intersect-queries [query-a query-b]
  (println "intersecting queries" query-a query-b)
  (hash-map
    :from "Gene"
    :select "*"
    :constraintLogic "A and B"
    :where [(assoc (get-in query-a [:where 0]) :code "A")
            (assoc (get-in query-b [:where 0]) :code "B")]))

(defn combine-queries [query-a query-b]
  (println "combining queries" query-a query-b)
  (hash-map
    :from "Gene"
    :select "*"
    :constraintLogic "A or B"
    :where [(assoc (get-in query-a [:where 0]) :code "A")
            (assoc (get-in query-b [:where 0]) :code "B")]))




;(defn readdress [queries op]
;  (loop [[current-query & remaining-queries] queries
;         alphabet    alphabet
;         final-query {:from "Gene" :select "*" :where []}]
;    (let [[allocated-letters remaining-letters] (split-at (count (:where current-query)) alphabet)
;          adjusted-query (-> (update current-query :where #(mapv assoc-letter % alphabet))
;                             ; Rebuild the constraint logic
;                             (update :constraintLogic #(clojure.string/join " " (interpose "AND" allocated-letters))))
;          _ (println "adjusted" adjusted-query)
;          updated-final-query (update final-query :constraintLogic (fn [val]
;                                                                     (println "val" val)
;                                                                     (let [new (str "(" (:constraintLogic adjusted-query) ")")]
;                                                                       (if (nil? val) new (str val " AND " new)))))]
;      (if (empty? remaining-queries)
;        updated-final-query
;        (recur remaining-queries remaining-letters updated-final-query)))))




(defn assoc-letter [constraint letter] (assoc constraint :code letter))

(defn readdress [queries op]
  (loop [[current-query & remaining-queries] queries
         alphabet         alphabet
         adjusted-queries '()]
    (let [[allocated-letters remaining-letters] (split-at (count (:where current-query)) alphabet)
          adjusted-query (-> (update current-query :where #(mapv assoc-letter % alphabet))
                             (update :constraintLogic #(clojure.string/join " " (interpose "AND" allocated-letters))))
          tally          (conj adjusted-queries adjusted-query)]
      (if (empty? remaining-queries)
        tally
        (recur remaining-queries remaining-letters tally)))))

(defn merge-queries [queries op]
  (let [queries (readdress queries op)]
    (println "queries" queries)
    {:select          "*"
     :from            "Gene"
     :constraintLogic (clojure.string/join " " (interpose op (map #(str "(" (:constraintLogic %) ")") queries)))
     :where           (into [] (mapcat (fn [query] (:where query)) queries))}))


(defn infix->prefix [[a op b & remaining]]
  (map (fn [x] (if (list? x) (infix->prefix x) x)) (into [op a b] remaining)))


(defn asymmetric-left-queries [query-a query-b]
  ;(println "asymmetric-left-queries" query-a query-b)

  (println "readdressed" (merge-queries [query-a query-b] "AND"))

  (hash-map
    :from "Gene"
    :select "*"
    :constraintLogic "A and B"
    :where [(assoc (get-in query-a [:where 0]) :code "A")
            (assoc (get-in query-b [:where 0]) :code "B" :op "NOT IN")]))

(defn asymmetric-right-queries [query-a query-b]
  (println "asymmetric-left-queries" query-a query-b)
  (hash-map
    :from "Gene"
    :select "*"
    :constraintLogic "A and B"
    :where [(assoc (get-in query-a [:where 0]) :code "A" :op "NOT IN")
            (assoc (get-in query-b [:where 0]) :code "B")]))



(re-frame/register-handler
  :determine-qop trim-v
  (fn [db]
    (let [states (get-in db [:projects (:active-project db) :query-operations :states])]
      (println "looking at states" states)
      (assoc-in db [:projects (:active-project db) :query-operations :operation]
                (cond
                  (and (= true (-> states :1 :keep))
                       (= true (-> states :2 :keep))
                       (= true (-> states :middle))) "union"
                  (and (= true (-> states :1 :keep))
                       (= true (-> states :2 :keep))
                       (= false (-> states :middle))) "subtract"
                  (and (= false (-> states :1 :keep))
                       (= true (-> states :2 :keep))
                       (= false (-> states :middle))) "asymmetric-right"
                  (and (= true (-> states :1 :keep))
                       (= false (-> states :2 :keep))
                       (= false (-> states :middle))) "asymmetric-left"
                  (and (= false (-> states :1 :keep))
                       (= false (-> states :2 :keep))
                       (= true (-> states :middle))) "intersection")))))



(register-handler
  :accept-qop trim-v
  (fn [db [s1 s2]]
    (let [uuid        (keyword (rid))
          op (get-in db [:projects (:active-project db) :query-operations :operation])
          update-path [:projects (:active-project db) :saved-data uuid]]

      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :editing true
                 :when (.now js/Date)
                 :payload {:service {:root "www.flymine.org/query"}
                           :data    {:format  "query"
                                     :type    "Gene"
                                     :payload {:from "Gene"
                                               :select "*"
                                               :where [{:path "Gene.id"
                                                        :op "ONE OF"
                                                        :values (case op
                                                                 "union" (union s1 s2)
                                                                 "intersection" (intersection s1 s2)
                                                                 "asymmetric-left" (difference s1 s2)
                                                                 "asymmetric-right" (difference s2 s1)
                                                                 "subtract" (union (difference s1 s2) (difference s2 s1)))}]}}}))))

(re-frame/register-handler
  :run-qop trim-v
  (fn [db]
    (let [uuid        (keyword (rid))
          update-path [:projects (:active-project db) :saved-data uuid]
          t1id        (get-in db [:projects (:active-project db)
                                  :query-operations :targets :1 :query])
          t2id        (get-in db [:projects (:active-project db)
                                  :query-operations :targets :2 :query])]

      (let [channels (map (partial im/query-rows {:service {:root "www.flymine.org/query"}}) [t1id t2id])]
        (go (let [result1 (set (flatten (<! (first channels))))
                  result2 (set (flatten (<! (second channels))))]
              (re-frame/dispatch [:accept-qop result1 result2]))))


      db)))


(re-frame/register-handler
  :save-qop
  trim-v
  (fn [db [data-to-save]]
    (println "saving qop" data-to-save)
    (let [uuid        (keyword (rid))
          update-path [:projects (:active-project db) :saved-data uuid]]
      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :editing true
                 :when (.now js/Date)
                 :payload data-to-save))))