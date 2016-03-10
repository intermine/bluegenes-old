(ns bluegenes.utils.layouthelpers)

(defn scroll-to [node]
  ;when reversing:  (.animate (js/$ "body") #js{:scrollTop 0} 500 "swing")
   (.animate (js/$ "body") #js{:scrollTop (-> (js/$ node) .offset .-top)} 500 "swing"))
