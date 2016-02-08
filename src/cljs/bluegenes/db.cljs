(ns bluegenes.db)

(def default-db
  {:name "βlueGenes"
   :whoami {:authenticated false}

   :available-data {}
   :active-history nil
   :dimmer true

   :mines {:flymine {:root "www.flymine.org/query"
                     :token nil}

           :humanmine {:root "www.humanmine.org/humanmine"
                       :token nil}

           :default {:root "www.humanmine.org/humanmine"
                     :token nil}}

   :histories {:z {:name "Local History X"
                   :slug "local-history-x"
                   :description "I come from the land of clojurescript."
                   :steps {:a {:tool "chooselist"
                               :_id :a
                               :title "List Chooser"
                               :description "List Chooser Description"
                               :state []}
                           :b {:tool "runtemplate"
                               :_id :b
                               :title "Show Results"
                               :description "Show List Results"
                               :state []
                               :subscribe [:a]}
                          ;  :c {:tool "idresolver"
                          ;      :title "Show Results"
                          ;      :description "Show BANANA Results"
                          ;      :state []
                          ;      :subscribe [:b]}
                          ;  :c {:tool "enrichment"
                          ;      :title "Show Enrichment"
                          ;      :description "Show Enrichment Results"
                          ;      :state []
                          ;      :subscribe [:b]}
                           }}
               :y {:name "Local History Y"
                   :description "I too was born in app-db."
                   :steps {:a {:tool "idresolution"
                               :title "List Chooser"
                               :description "List Chooser Description"
                               :state []}
                           :b {:tool "showresults"
                               :title "Show Results"
                               :description "Show List Results"
                               :state []
                               :subscribe [:a]}
                           :c {:tool "enrichment"
                               :title "Show Enrichment"
                               :description "Show Enrichment Results"
                               :subscribe [:b]
                               :state []}}}}})
