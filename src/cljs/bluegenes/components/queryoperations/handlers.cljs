(ns bluegenes.components.queryoperations.handlers
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
            [cuerdas.core :as cue]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report
                                logf tracef debugf infof warnf errorf fatalf reportf
                                spy get-env log-env)])
  (:use [cljs-uuid-utils.core :only [make-random-uuid]]))

(defn rid [] (str (make-random-uuid)))

(re-frame/register-handler
  :set-qop trim-v
  (fn [db [position id]]
    (update-in db
               [:projects (:active-project db)
                :query-operations :targets (keyword (str position))]
               assoc
               :id id
               :keep false)))

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

(defn asymmetric-left-queries [query-a query-b]
  (println "asymmetric-left-queries" query-a query-b)
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
      ;
      ;(def subtraction-query
      ;  {:select          "*"
      ;   :from            "Gene"
      ;   :constraintLogic "(A AND B) OR (C AND D)"
      ;   :where           [{:path  "Gene"
      ;                      :op    "IN"
      ;                      :value "FlyMine_AlzheimersUseCase"
      ;                      :code  "A"}
      ;                     {:path  "Gene"
      ;                      :op    "NOT IN"
      ;                      :value "PL FlyTF_putativeTFs"
      ;                      :code  "B"}
      ;                     {:path  "Gene"
      ;                      :op    "IN"
      ;                      :value "PL FlyTF_putativeTFs"
      ;                      :code  "C"}
      ;                     {:path  "Gene"
      ;                      :op    "NOT IN"
      ;                      :value "FlyMine_AlzheimersUseCase"
      ;                      :code  "D"}]})
      ;
      ;(go (println "query" (<! (im/query-count {:root "www.flymine.org/query"} q1))))

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


(re-frame/register-handler
  :run-qop trim-v
  (fn [db]




    (let [uuid        (keyword (rid))
          update-path [:projects (:active-project db) :saved-data uuid]
          t1id        (get-in db [:projects (:active-project db)
                                  :query-operations :targets :1 :id])
          t2id        (get-in db [:projects (:active-project db)
                                  :query-operations :targets :2 :id])
          t1          (get-in db [:projects (:active-project db)
                                  :saved-data t1id])
          t2          (get-in db [:projects (:active-project db)
                                  :saved-data t2id])
          op          (get-in db [:projects (:active-project db)
                                  :query-operations :operation])]

      (println "HAS AN OP" op)

      (println "payload" (case op
                           "union" (combine-queries
                                     (get-in t1 [:payload :data :payload])
                                     (get-in t2 [:payload :data :payload]))
                           "intersection" (intersect-queries
                                            (get-in t1 [:payload :data :payload])
                                            (get-in t2 [:payload :data :payload]))
                           "asymmetric-left" (asymmetric-left-queries
                                               (get-in t1 [:payload :data :payload])
                                               (get-in t2 [:payload :data :payload]))
                           "asymmetric-right" (asymmetric-right-queries
                                                (get-in t1 [:payload :data :payload])
                                                (get-in t2 [:payload :data :payload]))
                           "subtract" nil))

      (println "OP" op)

      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :editing true
                 :when (.now js/Date)
                 :payload {:service {:root "www.flymine.org/query"}
                           :data    {:format  "query"
                                     :type    "Gene"
                                     :payload (case op
                                                "union" (combine-queries
                                                          (get-in t1 [:payload :data :payload])
                                                          (get-in t2 [:payload :data :payload]))
                                                "intersection" (intersect-queries
                                                                 (get-in t1 [:payload :data :payload])
                                                                 (get-in t2 [:payload :data :payload]))
                                                "asymmetric-left" (asymmetric-left-queries
                                                                    (get-in t1 [:payload :data :payload])
                                                                    (get-in t2 [:payload :data :payload]))
                                                "asymmetric-right" (asymmetric-right-queries
                                                                     (get-in t1 [:payload :data :payload])
                                                                     (get-in t2 [:payload :data :payload]))
                                                "subtract" nil)}}))))


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