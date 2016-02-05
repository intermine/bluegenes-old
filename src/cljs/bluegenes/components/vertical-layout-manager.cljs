(ns bluegenes.components.vertical-layout-manager)

(defn remove-slider-classes [dom-node]
  "Remove the classes used to animate the sliding container"
  (.remove (.-classList dom-node) "growingUp")
  (.remove (.-classList dom-node) "babyContainer"))

(defn slide-in-tool [dom-node]
  "The first render of a tool slides it in from the top"
  (.add (.-classList dom-node) "growingUp")
  (.setTimeout js/window #(remove-slider-classes dom-node) 2000))

(defn get-tool-name [element]
  "temp debug method to get a tool name from the html element"
  (.-innerHTML (.querySelector element "a")))

(defn get-doc-height []
  "return the height of the entire html document including what's offscreen.
   With thanks to http://stackoverflow.com/questions/1145850/how-to-get-height-of-entire-document-with-javascript"
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max
      (.-scrollHeight body)
      (.-offsetHeight body)
      (.-clientHeight html)
      (.-scrollHeight html)
      (.-offsetHeight html))))

(defn viewport-distance-from-bottom []
  (- (get-doc-height) (aget js/document "body" "scrollTop")))

(defn in-view? [element]
  "Is the element in question in the viewport? cljs version of http://stackoverflow.com/questions/123999/how-to-tell-if-a-dom-element-is-visible-in-the-current-viewport?lq=1"
  (let [viewport (.getBoundingClientRect element)]
    (and
      (>= (.-top viewport) 0)
      (>= (.-left viewport) 0)
      (<= (.-bottom viewport (or (.-innerHeight js/window) (aget js/document "documentElement" "clientHeight"))))
      (<= (.-right viewport  (or (.-innerHeight js/window) (aget js/document "documentElement" "clientWidth")))))))

(defn stable-viewport []
  "Tools re-rendering above the current viewport can result in the content jumping.
So let's check if the element is IN the viewport right now. If it IS, just re-render. If not, count the distance from the bottom and re-focus the tool there."
  ;(.log js/console "%csetting scrolltop to" "background:turquoise;font-weight:bold;" (- (viewport-distance-from-bottom) window-location))
  (aset js/document "body" "scrollTop"
        (- (get-doc-height) window-location)))

(defn store-window-location! []
  ;(.log js/console "%cSaving window position" "color:hotpink;font-weight:bold;" (viewport-distance-from-bottom))
  (set! window-location (viewport-distance-from-bottom)))
