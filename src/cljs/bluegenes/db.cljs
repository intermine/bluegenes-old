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

   :histories {:k {:name "Local History Y"
                   :slug "local-history-y"
                   :description "I too was born in app-db."
                   ;  :structure [:a1 [:b1 :b2] :c1 :d1 [:e1 :e2 :e3 :e4]]
                   :structure [:a1 [:z1 :z2 :z3]]

                   :steps {:a1 {:_id :a1
                                :tool "chooselistcompact"}
                           :z1 {:_id :z1
                                :tool "enrichment"
                                :state [{:widget "go_enrichment_for_gene"
                                         :title "GO Enrichment"}]
                                :subscribe [:a1]}
                           :z3 {:_id :z3
                                :tool "enrichment"
                                :state [{:widget "publication_enrichment"
                                         :title "Publication Enrichment"}]
                                :subscribe [:a1]}
                           :z2 {:_id :z2
                                :tool "enrichment"
                                :loading? true
                                :state [{:widget "pathway_enrichment"
                                         :title "Pathway Enrichment"}]
                                :subscribe [:a1]}
                           :z4 {:_id :z4
                                :tool "enrichment"

                                :state [{:widget "prot_dom_enrichment_for_gene"
                                         :title "Protein Domain Enrichment"}]
                                :subscribe [:a1]}
                           :b1 {:_id :b1
                                :tool "echotool"
                                :subscribe [:a1]}
                           :b2 {:_id :b2
                                :tool "echotool"
                                :subscribe [:a1]}
                           :c1 {:_id :c1
                                :tool "echotool"
                                :subscribe [:b1]}
                           :d1 {:_id :d1
                                :tool "echotool"
                                :subscribe [:c1]}
                           :e1 {:_id :e1
                                :tool "echotool"
                                :subscribe [:d1]}
                           :e2 {:_id :e2
                                :tool "echotool"
                                :subscribe [:d1]}
                           :e3 {:_id :e3
                                :tool "echotool"
                                :subscribe [:d1]}
                           :e4 {:_id :e4
                                :tool "echotool"
                                :subscribe [:d1]}}}}})
