(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            ; [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [cljs.contrib.pprint :refer [pprint]]))

(def window-location (reagent/atom 0))

; TODO: The following three functions are passed to tools as a way to communicate
; with the rest of the application. Move them to a different namespace.

(defn append-state [tool data]
  "Append a tool's state to the previous states."
  (re-frame/dispatch [:append-state (keyword (:uuid tool)) data]))

(defn replace-state [tool data]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:replace-state (:uuid tool) data]))

(defn has-something [tool data]
  "Notify the world that the tool has consumable data."
  (re-frame/dispatch [:has-something (keyword (:uuid tool)) data]))

(defn tool-dimmer []
  "Loading screen that can be applied over a step. TODO: Move to components namespace."
  [:div.dimmer
   [:div.message
    [:div.loader]]])

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
  (.log js/console "%csetting scrolltop to" "background:turquoise;font-weight:bold;" (- (viewport-distance-from-bottom) window-location) "docheight: " (get-doc-height) "windowlocation" window-location)
  (aset js/document "body" "scrollTop"
        (- (get-doc-height) window-location)))


(defn step []
  "A generic component that houses a step in the history. Using the supplied tool name,
  it constructs a child component and passes it the tool's known state."
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
     {:display-name "step"
     :component-did-mount (fn [this]
       "Slide the tool in gracefully"
       (let [dn (.getDOMNode this)]
         (slide-in-tool dn)))

      :component-will-update (fn []
        "save the current screen position to prevent re-render jumps"
        (.log js/console "%cSaving window position" "color:hotpink;font-weight:bold;" (viewport-distance-from-bottom))
        (set! window-location (viewport-distance-from-bottom)))


      :component-did-update (fn [this]
        (let [element (.getDOMNode this)]
          (if (not (in-view? element))
            (stable-viewport)
            (.log js/console "is in view" (get-tool-name element)))))


      :reagent-render (fn [step-data]
                        (.debug js/console "Loading Step:" (clj->js step-data))
                        (let [_ nil]
                          [:section.step-container.babyContainer
                            [:header.toolbar
                             [:ul
                              [:li {:class (if (= @current-tab nil) "active")} [:a {:on-click #(swap-tab nil)} (:tool step-data)]]
                              [:li {:class (if (= @current-tab "data") "active")} [:a {:data-target "test"
                               :on-click    #(swap-tab "data")} "Data"]]]]
                            [:div.body
                             ; Show the contents of the selected tab
                             (cond
                               (= nil @current-tab)
                               (do
                                 [(-> bluegenes.tools (aget (:tool step-data)) (aget "core") (aget "main"))
                                  step-data {:append-state (partial append-state step-data)
                                             :replace-state (partial replace-state step-data)
                                             :has-something (partial has-something step-data)}])
                               (= "data" @current-tab)
                               (json-html/edn->hiccup step-data))
                              ; [tool-dimmer]
                             ]
                            ]))})))

(defn step-tree [steps]
  "Serialize the steps of a history in order of notification.
  Assume that if a tool exists and no other tool notifies it,
  then this tool is the starting point of the history."
  (let [all-notifiers (remove nil? (map (fn [[step value]] (:notify value)) steps))
        [starting-point] (utils/diff all-notifiers (keys steps))]
    (loop [id starting-point
           step-vec []]
      (if-not (contains? (id steps) :notify)
        (do
          (conj step-vec (id steps)))
        (recur (:notify (id steps)) (conj step-vec (id steps)))))))


(defn previous-steps []
  "Iterates over steps in the history and creates a step component for each."
  (let [steps-reaction (re-frame/subscribe [:steps])
        mines (re-frame/subscribe [:mines])]
    (let [steps @steps-reaction]
      (if (nil? steps)
        [:h1 "New history"]
        (into [:div] (for [s (reverse (step-tree steps))]
                       (do ^{:key (:uuid s)} [step (assoc s :mines @mines) nil])))))))

(defn history-details []
  "Not used as of yet."
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
       [:h2 (:name @history)]
       [:h4 (:description @history)]]))

(defn main-view []
    [:div
     [nextsteps/main]
     [previous-steps]])
