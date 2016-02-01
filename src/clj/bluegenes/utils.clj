(ns bluegenes.utils
  (:require [bluegenes.webservice.history.handlers :as db-handlers]))

(def seed-data
  {:histories [{:name "Test History 1"
                :description "I am a test history."
                :steps {:tool "chooselist"
                        :title "List Chooser"
                        :description "List Chooser Description"
                        :state []
                        :notify {:tool "viewtemplate"
                                 :title "Template Chooser"
                                 :description "Choose Template Description"
                                 :state []}}}
               {:name "Test History 2"
                :description "Description of second history."
                :steps {:tool "enrichment"
                        :title "Enrichment Chooser"
                        :description "Enrichment Description"
                        :state []
                        :notify {:tool "viewresults"
                                 :title "Table Viewer"
                                 :description "Table Viewer Description"
                                 :state []}}}]})


(defn seed []
  (println "SEEDING")
  (doall
    (for [history (:histories seed-data)]
      (let [history-id (:_id (db-handlers/create-history history))]
        (loop [step-data (:steps history)
               parent nil]
          (println "after update" (db-handlers/add-step history-id step-data)))))))





(defn seedold []
  (println "seeding")
  (db-handlers/create-history {:name "THIS IS A TEST"
                               :description "Something else"})
  {"1d4183ab-ef64-440d-8e1d-1c27423ef395" {:uuid "1d4183ab-ef64-440d-8e1d-1c27423ef395"
                                                      :name "Start by choosing a list."
                                                      :slug "history-1"
                                                      :description "Test history one."
                                                      :starting-point "e42bdcbf-0256-410a-994f-3e45fbed3952"
                                                      :steps {:e42bdcbf-0256-410a-994f-3e45fbed3952 {:tool        "chooselist"
                                                                                                     :uuid        "e42bdcbf-0256-410a-994f-3e45fbed3952"
                                                                                                     :title       "List Chooser"
                                                                                                     :description "Select a list."
                                                                                                     :has nil
                                                                                                     :settled     true
                                                                                                     :input        nil
                                                                                                     :state       []}}}})
