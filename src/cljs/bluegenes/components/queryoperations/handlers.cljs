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
                :networks (:active-network db)
                :query-operations (keyword (str position))]
               assoc
               :id id
               :keep false)))

(re-frame/register-handler
  :toggle-qop trim-v
  (fn [db [position]]
    (println "toggling position" position)
    (let [location [:projects (:active-project db)
                    :networks (:active-network db)
                    :query-operations (keyword (str position))
                    :keep]]
      (update-in db location not))))


(re-frame/register-handler
  :set-qop-op trim-v
  (fn [db [value]]
    (assoc-in db [:projects (:active-project db)
                  :networks (:active-network db)
                  :query-operations :operation] value)))


(defn intersect-queries [query-a query-b]
  (println "combining queries" query-a query-b)
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


(re-frame/register-handler
  :run-qop trim-v
  (fn [db]
    (let [uuid (keyword (rid))
          update-path [:projects (:active-project db) :saved-data uuid]
          t1id (get-in db [:projects (:active-project db)
                           :networks (:active-network db)
                           :query-operations :1])
          t2id (get-in db [:projects (:active-project db)
                           :networks (:active-network db)
                           :query-operations :2])
          t1 (get-in db [:projects (:active-project db)
                         :saved-data t1id])
          t2 (get-in db [:projects (:active-project db)
                         :saved-data t2id])
          op (get-in db [:projects (:active-project db)
                         :networks (:active-network db)
                         :query-operations :operation])]

      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :editing true
                 :when (.now js/Date)
                 :payload {:service {:root "www.flymine.org/query"}
                           :data    {:format  "query"
                                     :type    "Gene"
                                     :payload (case op
                                                "combine" (combine-queries
                                                            (get-in t1 [:payload :data :payload])
                                                            (get-in t2 [:payload :data :payload]))
                                                "intersect" (intersect-queries
                                                              (get-in t1 [:payload :data :payload])
                                                              (get-in t2 [:payload :data :payload])))}}))))


(re-frame/register-handler
  :save-qop
  trim-v
  (fn [db [data-to-save]]
    (println "saving qop" data-to-save)
    (let [uuid (keyword (rid))
          update-path [:projects (:active-project db) :saved-data uuid]]
      (update-in db update-path assoc
                 :label "TBD"
                 :_id uuid
                 :editing true
                 :when (.now js/Date)
                 :payload data-to-save))))