(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            ; [bluegenes.components.dimmer :as dimmer]
            ; [ajax.core :refer [GET POST]]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [bluegenes.components.vertical :as vertical]
            [cljs.contrib.pprint :refer [pprint]]))

(def window-location (reagent/atom 0))

; TODO: The following three functions are passed to tools as a way to communicate
; with the rest of the application. Move them to a different namespace.

(defn append-state [tool data]
  "Append a tool's state to the previous states."
  (re-frame/dispatch [:append-state (keyword (:_id tool)) data]))

(defn replace-state [tool data]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:replace-state (:_id tool) data]))

(defn has-something [tool data]
  "Notify the world that the tool has consumable data."
  (re-frame/dispatch [:has-something (keyword (:_id tool)) data]))

(defn tool-dimmer []
  "Loading screen that can be applied over a step. TODO: Move to components namespace."
  [:div.dimmer
   [:div.message
    [:div.loader]]])




(defn step [step-data]
  "A generic component that houses a step in the history. Using the supplied tool name,
  it constructs a child component and passes it the tool's known state."
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))
        parent-step (re-frame/subscribe [:to-step (first (:subscribe step-data))])]
    (.log js/console "subscribed to step " (clj->js @parent-step))
    (reagent/create-class
     {:display-name "step"
     :component-did-mount (fn [this]
       "Slide the tool in gracefully and store vertical window location to prevent render jumps"
         (vertical/store-window-location!)
         (vertical/slide-in-tool (.getDOMNode this)))

      :component-will-update (fn []
        "save the current screen position to prevent re-render jumps"
        (vertical/store-window-location!))


      :component-did-update (fn [this]
        (if (not (vertical/in-view? (.querySelector js/document ".next-steps")))
          "stabilise viewport to prevent UI jumps."
          (vertical/stable-viewport)))

      ; Keeping for now as this worked in the past for monitoring DOM changes:
      ; :component-did-mount (fn [this]
      ;                        "Reposition all tools if this tool's DOM has mutated"
      ;                        (let [dn (.getDOMNode this)]
      ;                          (.bind (js/$ dn) "DOMSubtreeModified" position-all-tools)))

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
                                  (assoc step-data :input (:produced @parent-step)) {:append-state (partial append-state step-data)
                                             :replace-state (partial replace-state step-data)
                                             :has-something (partial has-something step-data)}])
                               (= "data" @current-tab)
                               (json-html/edn->hiccup (assoc step-data :input (:produced @parent-step))))
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

(defn step-tree-subscribe [steps]
  "Serialize the steps of a history.
  Assume that a tool with no subscription is a starting point. Then recursively
  find other steps that subscribe to the starting point. Subscriptions are
  vectors (meaning that a step can subscribe to more than one step), but for now
  this only supports one item in the subscription (hence the 'first' function.)"
  (let [[starting-point-id] (first (filter (fn [[step value]] (nil? (:subscribe value))) steps))
        find-children (fn [parent-id]
                        (filter (fn [[step value]]
                                  (not (nil? (some #{parent-id} (:subscribe value)))))
                                steps))]
    (loop [id starting-point-id
           step-vec []]
      (let [[children] (find-children id)]
        (if (nil? children)
          (conj step-vec (id steps))
          (recur (first children) (conj step-vec (id steps))))))))

(defn previous-steps []
  "Iterates over steps in the history and creates a step component for each."
  (let [steps-reaction (re-frame/subscribe [:steps])
        mines (re-frame/subscribe [:mines])]
    (let [steps @steps-reaction]
      (.log js/console "steps reaction subscription" (clj->js (step-tree-subscribe steps)))
      (if (nil? steps)
        [:h1 "New history"]
        (into [:div] (for [s (reverse (step-tree-subscribe steps))]
                       (do ^{:key (:_id s)} [step (assoc s :mines @mines) nil])))))))

(defn history-details []
  "Not used as of yet."
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
       [:h2 (:name @history)]
       [:h4 (:description @history)]]))

; (GET "http://localhost:3449/api/history/394537ae-b4eb-4b13-a78d-edadbd11a6f8/steps"
;      {:keywords? true
;       :response-format :json
;       :handler (fn [response] "res" (println response))})

(defn main-view []
    [:div
     [nextsteps/main]
     [previous-steps]])
