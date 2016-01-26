(ns bluegenes.toolmap)

(def toolmap
  {"faketool" {:accepts {:format "list"
                         :type "Gene"}}
   "enrichtool" {:accepts {:format "list"
                         :type "Gene"}}
   "runtemplate" {:accepts {:format "list"
                         :type "Gene"}}
   "idresolver" {:accepts {:format "list"
                         :type "Gene"}}
   "showlist" {:accepts {:format "list"
                           :type "Gene"}}})
