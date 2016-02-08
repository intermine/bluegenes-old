(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            ; [bluegenes.components.dimmer :as dimmer]
            ; [ajax.core :refer [GET POST]]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [bluegenes.components.vertical-layout-manager :as vertical]))


(enable-console-print!)

(def window-location (reagent/atom 0))

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

(defn append-state [tool data]
  "Append a tool's state to the previous states."
  (.log js/console "append state caled " (clj->js data) )
  (re-frame/dispatch [:append-state (keyword (:_id tool)) data]))

(defn replace-state [tool data]
  "Replace a tool's state with its current state."
  (re-frame/dispatch [:replace-state (:_id tool) data]))

(defn has-something [tool data]
  "Notify the world that the tool has consumable data."
  (re-frame/dispatch [:has-something (keyword (:_id tool)) data]))

(defn build-comms-map
  "Produce a bespoke map of functions for a tool to communicate
  with the framework."
  [step-data]
  {:append-state (partial append-state step-data)
   :replace-state (partial replace-state step-data)
   :has-something (partial has-something step-data)})

(defn single-step
  "Subscribe to a single step in the history and represent it visually. Also subscribes
  to an upstream step to have access to its input. "
  [step-data]
  (let [upstream-step-data (re-frame/subscribe [:to-step (first (:subscribe @step-data))])]
    (println "single step sees" step-data)
    (let [comms (build-comms-map @step-data)
          global-info nil
          tool-component (-> bluegenes.tools
                             (aget (:tool @step-data))
                             (aget "core")
                             (aget "main"))]
      [tool-component
       (last (:state @step-data))
       (:produced @upstream-step-data)
       global-info
       comms])))

(defn step-dashboard
  "Create a dashboard with a tool inside. The dashboard includes common
  functionality such as data tabs, notes, etc."
  [_id]
  (let [step-data (re-frame/subscribe [:to-step _id])
        current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
     {:reagent-render
      (fn []
        [:div
         [:div.step-container
          [:div.step-inner
           [:div.toolbar
            [:ul
             [:li {:class (if (= @current-tab nil) "active")}
              [:a {:on-click #(swap-tab nil)}
               (:tool @step-data)]]
             [:li {:class (if (= @current-tab "data") "active")}
              [:a {:data-target "test"
                   :on-click #(swap-tab "data")}
               "Data"]]]]
           [:div.body
            [:div {:className (if (= @current-tab "data") "hide")}
             [single-step step-data]]
            [:div {:className (if (= @current-tab nil) "hide")}
             (json-html/edn->hiccup @step-data)]]]]])})))

(defn previous-steps []
  (let [step-list (map :_id (step-tree-subscribe (deref (re-frame/subscribe [:steps]))))]
    (into [:div]
          (for [_id step-list]
            (do ^{:key (str "dashboard" _id)} [step-dashboard _id])))))

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
