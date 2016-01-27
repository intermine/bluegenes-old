(ns bluegenes.db)

(def default-db
  {:name           "βlueGenes"

   :available-data {}
   :active-history nil
   :dimmer true

   :mines {:flymine   {:root "www.flymine.org/query"
                       :token nil}

           :humanmine {:root "www.humanmine.org/humanmine"
                       :token nil}

           :default   {:root "www.humanmine.org/humanmine"
                       :token nil}}

   :histories {"1d4183ab-ef64-440d-8e1d-1c27423ef395" {:uuid "1d4183ab-ef64-440d-8e1d-1c27423ef395"
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
                                                                                                      :state       []}}}}})
