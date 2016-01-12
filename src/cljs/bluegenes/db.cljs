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
                                                       :name "New history with a starting point."
                                                       :slug "history-1"
                                                       :description "Select a list and then view its output."
                                                       :steps [{:tool        "chooselist"
                                                                :uuid        "e42bdcbf-0256-410a-994f-3e45fbed3952"
                                                                :title       "List Chooser"
                                                                :description "Select a list."
                                                                :has nil
                                                                :settled     true
                                                                :input        nil
                                                                :state       []}
                                                               {:tool        "showlist"
                                                                :uuid        "cb2af143-4cdd-4536-8e91-bba0b17e4126"
                                                                :title       "List Shower"
                                                                :description "View contents."
                                                                :has nil
                                                                :settled     true
                                                                :state       []}]}

               "d3893c7d-031c-452b-b85b-fc1757c383f9" {:uuid "d3893c7d-031c-452b-b85b-fc1757c383f9"
                                                       :slug "history-2"
                                                       :name "New history with different starting point, same endpoint."
                                                       :description "This workflow simulates an entry point with no initial input. User interaction required."
                                                       :steps [
                                                               {:tool        "idresolver"
                                                                :uuid        "f5da98e2-bec6-4f2f-802e-ab5c6cf035b4"
                                                                :title       "ID Resolver"
                                                                :description "Uploaded genes from my experiment."
                                                                :state       []
                                                                :settled     false
                                                                :input       nil}
                                                               {:tool        "showlist"
                                                                :uuid        "cb2af143-4cdd-4536-8e91-bba0b17e336"
                                                                :title       "List Shower"
                                                                :description "Viewed them in a list."
                                                                :has nil
                                                                :settled     true
                                                                :state       []}]}

               "a4c84b0d-332f-46bd-9fd8-0ddbf1983e29" {:uuid "a4c84b0d-332f-46bd-9fd8-0ddbf1983e29"
                                                       :slug "results-for-thesis"
                                                       :name "Two tools, state saved."
                                                       :description "Workflow that gets repeated when loaded. No user interaction required."
                                                       :steps [
                                                               {:tool        "idresolver"
                                                                :uuid        "f5da98e2-bec6-4f2f-802e-ab5c6cf035b4"
                                                                :title       "ID Resolver"
                                                                :description "Uploaded genes from my experiment."
                                                                :state       [{:input "mad, zen, ey"}]
                                                                :settled     false
                                                                :input       nil}
                                                               {:tool        "showlist"
                                                                :uuid        "cb2af143-4cdd-4536-8e91-bba0b17e336"
                                                                :title       "List Shower"
                                                                :description "Viewed them in a list."
                                                                :has nil
                                                                :settled     true
                                                                :state       []}]}}})
