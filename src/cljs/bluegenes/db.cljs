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

   :histories {:lists {:name "Gene Lists"
                   :slug "local-history-x"
                   :description "Browse Intermine Gene lists.s"
                   :steps {:choose-list {:tool "chooselist"
                               :_id :choose-list
                               :title "List Chooser"
                               :description "List Chooser Description"
                               :state []}
                           }}
              :homepage-list-upload
               {
                 :name "List Upload"
                 :slug "list-upload"
                 :description "Upload a list of genes, proteins, etc."
                 :steps {:id-resolver
                   {:tool "idresolver"
                    :_id :id-resolver
                   :title "Show Results"
                   :description "Show Results"
                   :state []}}}
              ; :y {:name "Local History Y"
              ;      :slug "local-history-y"
              ;      :description "I too was born in app-db."
              ;      :steps {:a {:tool "idresolver"
              ;                  :title "List Chooser"
              ;                  :description "List Chooser Description"
              ;                  :state []}
              ;              :b {:tool "showresults"
              ;                  :title "Show Results"
              ;                  :description "Show List Results"
              ;                  :state []
              ;                  :subscribe [:a]}
              ;              :c {:tool "enrichment"
              ;                  :title "Show Enrichment"
              ;                  :description "Show Enrichment Results"
              ;                  :subscribe [:b]
              ;                  :state []}}}
               }
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
