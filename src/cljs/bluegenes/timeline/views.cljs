(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            ; [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [cljs.contrib.pprint :refer [pprint]]))


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

(defn position-all-tools []
  "Position tools vertically based on the size of their predecessor(s).
  Combined with a CSS transition, this will produce a sliding effect when new tools
  are added to the history."
  (let [steps (.getElementsByClassName js/document "step-container")
        veced (into [] (map #(.item steps %) (range (.-length steps))))]
    (reduce (fn [total e]
                       (aset e "style" "transform" (str "translateY(" total "px)"))
                       (+ total (.-offsetHeight e)))
                     (.-offsetHeight (first veced)) (rest veced))))

(defn step []
  "A generic component that houses a step in the history. Using the supplied tool name,
  it constructs a child component and passes it the tool's known state."
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
     {:display-name "step"
      :component-did-mount (fn [this]
                             "Reposition all tools if this tool's DOM has mutated"
                             (let [dn (.getDOMNode this)]
                               (.bind (js/$ dn) "DOMSubtreeModified" position-all-tools)))

      :reagent-render (fn [step-data]
                        (.debug js/console "Loading Step:" (clj->js step-data))
                        (let [_ nil]
                          [:div.step-container
                           [:div.step-inner
                            [:div.toolbar
                             [:ul.nav.nav-tabs
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
                            ]]))})))

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
      [:div.step-inner
       [:h2 (:name @history)]
       [:h4 (:description @history)]]]))

(defn main-view []
    [:div
     [nextsteps/main]
     [previous-steps]])
