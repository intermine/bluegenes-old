(ns bluegenes.utils.layouthelpers)

(defn scroll-to [node]
  "Scrolls to a given node"
  ;when reversing:  (.animate (js/$ "body") #js{:scrollTop 0} 500 "swing")
   (.animate (js/$ "body") #js{:scrollTop (-> (js/$ node) .offset .-top)} 500 "swing"))

 (defn get-step-node [elem]
   "Given an element inside a step, navigate up the DOM tree to find the step-container parent. Helper for scroll-to-next-step"
   (let [p (.-parentNode elem)
         class-list (into #{} (array-seq (.-classList p)))]
     (if (contains? class-list "step-container")
     (.-parentNode elem)
     (get-step-node p))
   ))

(defn get-next-step-node [elem]
  "Return the next sibling of an element, or null if there is none. Helper for scroll-to-next-step"
  (.-nextElementSibling elem))

(defn scroll-to-next-step [elem-from-current-step]
  "Given a node within a step (used to identify which step you're currently in), scroll the NEXT step sibling into view. "
  (let [this-node (get-step-node elem-from-current-step)
        next-node (get-next-step-node this-node)]
    (scroll-to next-node)))
