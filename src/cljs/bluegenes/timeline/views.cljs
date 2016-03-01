(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            [bluegenes.timeline.api :as timeline-api]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.components.stepdash.core :as stepdash]
            [bluegenes.utils :as utils]
            [bluegenes.components.vertical :as vertical]
            [reagent.impl.util :as impl :refer [extract-props]]))

(enable-console-print!)

(defn tool-dimmer []
  "Loading screen that can be applied over a step. TODO: Move to components namespace."
  [:div.dimmer
   [:div.message
    [:div.loader]]])

(defn step-tree [steps]
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

(defn step-graph-wip [steps]
  "Serialize the steps of a history.
  Assume that a tool with no subscription is a starting point. Then recursively
  find other steps that subscribe to the starting point. Subscriptions are
  vectors (meaning that a step can subscribe to more than one step), but for now
  this only supports one item in the subscription (hence the 'first' function.)"
  (let [[starting-point-id] (first (filter (fn [[step value]] (nil? (:subscribe value))) steps))
        find-downstream (fn [parent-id]
                        (filter (fn [[step value]]
                                  (not (nil? (some #{parent-id} (:subscribe value)))))
                                steps))]
    (loop [id starting-point-id
           step-vec []]
      (let [downstream (find-downstream id)]
        (println "downstream" downstream)
        (if (nil? downstream)
          (conj step-vec [(id steps)])
          (recur (first downstream) (conj step-vec (id steps))))))))



(defn step
  "Subscribe to a single step in the history and represent it visually. Also subscribes
  to an upstream step to have access to its input. "
  [step-args]
  (let [upstream-step-data (re-frame/subscribe [:to-step (first (:subscribe step-args))])
        api (timeline-api/build-api-map step-args)]
    (fn [step-data]
      (let [global-info nil
            tool-component (-> bluegenes.tools
                               (aget (:tool step-data))
                               (aget "core")
                               (aget "main"))]
        [tool-component
         {:state (last (:state step-data))
          :upstream-data (:produced @upstream-step-data)
          :global-data global-info
          :api api}]))))

(defn step-container
  "Create a dashboard with a tool inside. The dashboard includes common
  functionality such as data tabs, notes, etc."
  [_id]
  (let [step-data (re-frame/subscribe [:to-step _id])
        current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
     {:reagent-render (fn [_id]
                        [:div
                         [:div.step-container
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
                            [step @step-data]]
                           [:div {:className (if (= @current-tab nil) "hide")}
                            (json-html/edn->hiccup @step-data)]
                           (if (:loading? @step-data) [tool-dimmer])]]])})))

(defn steps-dashboard
  "Create a dashboard with a tool inside. The dashboard includes common
  functionality such as data tabs, notes, etc."
  [ids]
  (let [_ nil]
    (reagent/create-class
     {:reagent-render (fn [ids]
                        [:div
                         [:div.step-container
                          [:div.body
                           [:h1 "Step Dashboard"]
                           [:div.fl-row
                           (for [id ids]
                             [:div.fl-cell ^{:key (str "step-container" id)} [step-container id]]
                             )]]]])})))

(defn previous-steps
  "Iterate through the history's structure and create step containers for
  single tools (keyword) or steps dashboards for grouped tools (vector)."
  []
  (let [step-path (re-frame/subscribe [:step-path])]
    (fn []
      (into [:div.prevsteps]
            (for [id @step-path]
              (if (vector? id)
                ^{:key (str "group" (str id))} [steps-dashboard id]
                ^{:key (str "step-container" id)} [step-container id]))))))


(defn history-details []
  "Not used as of yet."
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
       [:h2 (:name @history)]
       [:h4 (:description @history)]]))

(defn main-view []
    [:div
     [stepdash/main]
    ;  [nextsteps/main]
     [previous-steps]])
