(ns bluegenes.tools.externallinks.config)

(def link-config
  {
  :Gene
  {
    :flybase
    { :name "Flybase"
      :url "http://flybase.org/reports/<<identifier>>.html"
      :identifier "primaryIdentifier"}
    :ncbi-entrez-gene
    { :name "NCBI Entrez Gene"
      :url "http://www.ncbi.nlm.nih.gov/gene/?term=<<identifier>>[uid]"}
      :identifier "?? where on earth does this identifier come from? ask rachel or julie"
  }})

  ;  xreflink.UniGene.url=http://www.ncbi.nlm.nih.gov/sites/entrez?db=unigene&term=<<attributeValue>>
  ;  xreflink.GENE3D.url=http://www.cathdb.info/cathnode/<<attributeValue>>
  ;  xreflink.RefSeq.url=http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?val=<<attributeValue>>
  ;  xreflink.HAMAP.url=http://www.expasy.org/unirule/<<attributeValue>>
  ;  xreflink.PANTHER.url=http://www.pantherdb.org/panther/family.do?clsAccession=<<attributeValue>>
  ;  xreflink.PFAM.url=http://pfam.sanger.ac.uk/family?<<attributeValue>>
  ;  xreflink.PIRSF.url=http://pir.georgetown.edu/cgi-bin/ipcSF?id=<<attributeValue>>
  ;  xreflink.PRINTS.url=http://www.bioinf.manchester.ac.uk/cgi-bin/dbbrowser/sprint/searchprintss.cgi?display_opts=Prints&category=None&queryform=false&prints_accn=<<attributeValue>>
  ;  xreflink.PRODOM.url=http://prodom.prabi.fr/prodom/current/cgi-bin/request.pl?question=DBEN&query=<<attributeValue>>
  ;  xreflink.PROFILE.url=http://expasy.org/prosite/<<attributeValue>>
  ;  xreflink.PROSITE.url=http://expasy.org/prosite/<<attributeValue>>
  ;  xreflink.SMART.url=http://smart.embl-heidelberg.de/smart/do_annotation.pl?ACC=<<attributeValue>>
  ;  xreflink.SSF.url=http://supfam.org/SUPERFAMILY/cgi-bin/scop.cgi?ipid=<<attributeValue>>
  ;  xreflink.TIGRFAMs.url=http://cmr.jcvi.org/cgi-bin/CMR/HmmReport.cgi?hmm_acc=<<attributeValue>>
  ;  xreflink.NCBI\ Entrez\ Gene\ identifiers.url=http://www.ncbi.nlm.nih.gov/gene/?term=<<attributeValue>>[uid]
  ;  xreflink.NCBI.url=http://www.ncbi.nlm.nih.gov/gquery/?term=<<attributeValue>>
  ;  xreflink.Ensembl.url=http://www.ensembl.org/Multi/Search/Results?species=all;idx=;q=<<attributeValue>>
  ;  xreflink.Vega.url=http://vega.sanger.ac.uk/Multi/Search/Results?species=all;idx=;q=<<attributeValue>>
  ;  xreflink.HGNC.url=http://www.genenames.org/data/hgnc_data.php?hgnc_id=<<attributeValue>>
