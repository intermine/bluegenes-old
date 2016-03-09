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

   :histories {:id4u {:name "Smart ID Resolver"
                      :slug "id-resolver"
                      :description "Browse and analyse lists"
                      :structure [:a1]
                      :steps {:a1 {:_id :a1
                                   :tool "smartidresolver"}}}

               :k {:name "List Analysis"
                   :slug "local-history-k"
                   :description "Browse and analyse lists"
                   :structure [:a1 [:z1 :z2 :z3 :z4 :z5 :z6]]
                   :steps {:a1 {:_id :a1
                                :tool "chooselistcompact"}
                           :z1 {:_id :z1
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "go_enrichment_for_gene"
                                         :correction "None"
                                         :title "Gene Ontology Enrichment"}]}
                           :z2 {:_id :z2
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "pathway_enrichment"
                                         :title "Pathway Enrichment"}]}
                           :z3 {:_id :z3
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "publication_enrichment"
                                         :title "Publication Enrichment"}]}
                           :z4 {:_id :z4
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "prot_dom_enrichment_for_gene"
                                         :title "Protein Domain Enrichment"}]}
                           :z5 {:_id :z5
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "miranda_enrichment"
                                         :title "MiRNA Enrichment"}]}
                           :z6 {:_id :z6
                                :tool "enrichment"
                                :subscribe [:a1]
                                :state [{:widget "bdgp_enrichment"
                                         :title "BDGP Enrichment"}]}
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
                                :subscribe [:d1]}}}

               :lists {:name "Gene Lists"
                       :slug "local-history-x"
                       :description "Browse Intermine Gene lists.s"
                       :steps {:choose-list {:tool "chooselist"
                                             :_id :choose-list
                                             :title "List Chooser"
                                             :description "List Chooser Description"
                                             :state []}}}

               :homepage-list-upload {:name "List Upload"
                                      :slug "list-upload"
                                      :description "Upload a list of genes, proteins, etc."
                                      :steps {:id-resolver
                                              {:tool "idresolver"
                                               :_id :id-resolver
                                               :title "Show Results"
                                               :description "Show Results"
                                               :state []}}}
               :search {:name "Search"
                        :slug "search"
                        :description "I too was born in app-db."
                        :structure [:search-tool [:summary1 :summary2]]
                        :steps {
                          :search-tool {
                            :tool "search"
                            :_id :search-tool
                            :title "Search InterMine"
                            :description "Search"
                            :state []}
                          :summary1 {
                            :subscribe [:search-tool]
                            :tool "summary"
                            :_id :summary1
                            :title "Summary"
                            :description "Summary"
                            :state []}
                          :summary2 {
                            :subscribe [:search-tool]
                            :tool "summary"
                            :_id :summary2
                            :title "Summary"
                            :description "Summary"
                            :state []}}}

               :homepage-template-histories {:a { :type "Gene"
                                                 :description "For the selected organism retrieve all genes
                                                 and their coding sequences, suitable for export as GFF3 or FASTA."
                                                 ;:steps {};TODO: add entry point for single gene. protein, organism?
                                                 }
                                             :b { :type "Protein"
                                                 :description "Show all the proteins
                                                 from a particular organism."}
                                             :c { :type "Gene"
                                                 :description "Show all alleles for a specific Drosophila gene.
                                                 Show all available information, eg mutagen,
                                                 allele class and phenotype linked to each allele."}}}})
