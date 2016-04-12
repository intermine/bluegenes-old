(ns bluegenes.db)

(def default-db
  {:name "βlueGenes"
   :whoami {:authenticated false}

   :cache {:templates {}
           :models {}
           :display-names {:flymine {:primaryIdentifier "Primary Identifier"}}}

   :available-data {}
   :active-history nil
   :dimmer true
   :search-term nil

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

   :histories {:id4u {:name "Smart ID Resolver"
                      :slug "id-resolver"
                      :description "Look up a list of identifiers, e.g. Genes Symbols"
                      :structure [:a1]
                      :steps {:a1 {:_id :a1
                                   :tool "smartidresolver"}
                              :b1 {:_id :b1
                                   :tool "enrichment"
                                   :subscribe [:a1]
                                   :state [{:widget "go_enrichment_for_gene"
                                            :correction "None"
                                            :title "Gene Ontology Enrichment"}]}
                              }}

               :k {:name "List Analysis"
                   :slug "list-analysis"
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
                                         :title "BDGP Enrichment"}]}}}

               :search {:name "Search"
                        :slug "search"
                        :description "Search across InterMine for just about anything."
                        :structure [:search-tool [:summary :minelinks :externallinks]]
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
                          :minelinks {
                            :subscribe [:search-tool]
                            :tool "minelinks"
                            :_id :minelinks
                            :title "Homologue Links"
                            :description "Homologue Links"
                            :state []}
                          :externallinks {
                            :subscribe [:search-tool]
                            :tool "externallinks"
                            :_id :externallinks
                            :title "Outbound Links"
                            :description "Outbound Links"
                            :state []}
                                }}
}})
