(ns bluegenes.tools.enrichment.controller)

(defn build-matches-query [query path-constraint identifier]
  (update-in query [:where]
             conj {:path path-constraint
                   :op "ONE OF"
                   :values [identifier]}))

(defn ncbi-link [identifier]
     (str "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids="
          identifier))
