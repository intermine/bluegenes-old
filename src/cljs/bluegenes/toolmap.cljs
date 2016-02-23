(ns bluegenes.toolmap)

(def tools
  [["viewtable" {:title "Inspect Results"
                 :accepts {:type "Gene"}}]
   ["runtemplate" {:title "Structured Search"
                   :accepts {:type "Gene"}}]])

; (somefunc :val 1 :another 2)
; (filter
;  (comp (fn [[k value-map]]
;    (println "k" k)
;    true) first)
;  tools)
