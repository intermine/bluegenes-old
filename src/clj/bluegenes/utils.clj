(ns bluegenes.utils
  (:require [bluegenes.webservice.history.handlers :as db-handlers]))


(def seed-data-nested
  {:histories {:z {:name "Test History 1"
                   :description "I am a test history."
                   :steps {:a {:tool "chooselist"
                               :title "List Chooser"
                               :description "List Chooser Description"
                               :state []}
                           :b {:tool "showresults"
                               :title "Show Results PLEASE"
                               :description "Show List Results"
                               :state [{:push 1 :shove "another"}
                                       {:push 9 :shove "thisone"}]
                               :subscribe [:a]}
                           :c {:tool "enrichment"
                               :title "Show Enrichment"
                               :description "Show Enrichment Results"
                               :state []
                               :subscribe [:b]}}}
               :y {:name "Test History 2"
                   :description "I am test history #2."
                   :steps {:a {:tool "idresolution"
                               :title "List Chooser"
                               :description "List Chooser Description"
                               :state []}
                           :b {:tool "showresults"
                               :title "Show Results"
                               :description "Show List Results"
                               :state [{:push 1 :shove "another"}
                                       {:push 9 :shove "thisone"}]
                               :subscribe [:a]}
                           :c {:tool "enrichment"
                               :title "Show Enrichment"
                               :description "Show Enrichment Results"
                               :subscribe [:b]
                               :state []}}}}})



(defn dump-history [id]
  (reduce
                          (fn [m step-data]
                            (update-in m [:steps] assoc
                                       (keyword (:_id step-data))
                                       (clojure.set/rename-keys
                                        step-data {:_id :id})))
                          (assoc (first (db-handlers/get-history id)) :steps {})
                          (db-handlers/get-steps (:steps (first (db-handlers/get-history id))))))


(defn seed []
  (doall
    (for [[temp-id history-data] (seq (:histories seed-data-nested))]
      ;Create a history and store its ID
      (let [history-id (:_id (db-handlers/create-history history-data))]
        ;Create a step for each step in the history and store its new ID
        (let [translation (reduce (fn [translation [tmp-id data]]
                          (assoc translation
                                 tmp-id
                                 (:_id (db-handlers/create-step data))))
                        {}
                        (:steps history-data))]
          (println "got table" translation)
          (doall
            (for [[tmp-id data] (:steps history-data)]
              (do
                (if-not (nil? (:subscribe data))
                    (db-handlers/update-step
                     (tmp-id translation)
                     {:subscribe ((first (:subscribe data)) translation)}))
                (db-handlers/add-step-to-history history-id (tmp-id translation))))))))))
