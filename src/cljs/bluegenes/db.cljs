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
                          ;  :b {:tool "runtemplate"
                          ;      :_id :b
                          ;      :title "Show Results"
                          ;      :description "Show List Results"
                          ;      :state []
                          ;      :subscribe [:a]}
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
                               :state []}}}}
   :homepage-template-histories
   {
      :a { :type "Gene"
        :description "For the selected organism retrieve all genes and their coding sequences, suitable for export as GFF3 or FASTA."
        ;:steps {};TODO: add entry point for single gene. protein, organism?
      }
      :b { :type "Protein"
        :description "Show all the proteins from a particular organism."
      }
      :c { :type "Gene"
        :description "Show all alleles for a specific Drosophila gene. Show all available information, eg mutagen, allele class and phenotype linked to each allele."
      }
    }
   })
