(ns bluegenes.db)

(def default-db
  {:name "βlueGenes"
   :whoami {:authenticated false}

   :available-data {}
   :active-history nil
   :dimmer true

   :remote-mines {
     :humanmine {
       :name "HumanMine"
       :url "http://www.humanmine.org/humanmine"
       :service {:root "http://www.humanmine.org/humanmine/service"}
       :organism "H. sapiens"}
     :yeastmine {
       :name "YeastMine"
       :url "http://yeastmine.yeastgenome.org/yeastmine"
       :service {:root "http://yeastmine.yeastgenome.org/yeastmine/service"}
       :organism "S. cerevisiae"}
     :zebrafishmine {
       :name "ZebraFishMine"
       :url "http://www.zebrafishmine.org"
       :service {:root "http://www.zebrafishmine.org"}
       :organism "D. rerio"}
     :ratmine {
       :name "RatMine"
       :url "http://ratmine.mcw.edu/ratmine"
       :service {:root "http://stearman.hmgc.mcw.edu/ratmine"}
       :organism "R. norvegicus"}
     :mousemine {
       :name "MouseMine"
       :url "http://www.mousemine.org/mousemine"
       :service {:root "http://www.mousemine.org/mousemine/service"}
       :organism "M. musculus"}
     :modmine {
       :name "ModMine"
       :url "http://intermine.modencode.org/release-33"
       :service {:root "http://intermine.modencode.org/release-33"}
       :organism "C. elegans"}}

   :histories {:k {:name "List Analysis"
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
                        :structure [:search-tool [:summary :outboundlinks]]
                        :steps {
                          :search-tool {
                            :tool "search"
                            :_id :search-tool
                            :title "Search InterMine"
                            :description "Search"
                            :state []}
                          :summary {
                            :subscribe [:search-tool]
                            :tool "summary"
                            :_id :summary
                            :title "Summary"
                            :description "Summary"
                            :state []}
                          :outboundlinks {
                            :subscribe [:search-tool]
                            :tool "outboundlinks"
                            :_id :outboundlinks
                            :title "Outbound Links"
                            :description "Outbound Links"
                            :state []}
                                }}

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
