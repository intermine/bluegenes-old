(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            [bluegenes.timeline.api :as timeline-api]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.components.stepdash.core :as stepdash]
            [bluegenes.utils.layouthelpers :as layout]
            [bluegenes.components.vertical :as vertical]
            [bluegenes.components.drawer.core :as drawer]
            [bluegenes.components.whatnext.core :as whatnext]
            [bluegenes.components.queryoperations.view :as query-operations]
            [bluegenes.tools.viewtable.core :as viewtable]
            [reagent.impl.util :as impl :refer [extract-props]]
            [bluegenes.api :as api]
            [bluegenes.components.savetodrawer.core :as savetodrawer]
            [bluegenes.components.lighttable.core :as lighttable]))

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
    (loop [id       starting-point-id
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
    (loop [id       starting-point-id
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
        api                (timeline-api/build-api-map step-args)]
    (fn [step-data]
      ;(println "upstream DATA" @upstream-step-data)
      (let [global-info    nil
            tool-component (-> bluegenes.tools
                               (aget (:tool step-data))
                               (aget "core")
                               (aget "main"))]
        [tool-component
         {:state         (last (:state step-data))
          :upstream-data (:produced @upstream-step-data)
          :global-data   global-info
          :api           api}]))))

(defn step-container
  "Create a container with a tool inside. The container includes common
  functionality such as data tabs, notes, etc."
  [_id & [in-grid]]
  (let [step-data   (re-frame/subscribe [:to-step _id])
        current-tab (reagent/atom nil)
        swap-tab    (fn [name] (reset! current-tab name))]
    (reagent/create-class
      {:component-did-mount (fn [this]
                              (let [node (reagent/dom-node this)]
                                ;(if (:scroll-to? @step-data)
                                ;  (layout/scroll-to node))
                                ))
       :reagent-render      (fn [_id]
                              [:div
                               {:class
                                ;(if-not in-grid "step-container")
                                (if-not (= "dashboard" (:tool @step-data)) "step-container")
                                }
                               ;(println "loading tool" (:tool @step-data))
                               ;[:div (str @step-data)]


                               ; [:div.toolbar
                               ;  [:ul
                               ;   [:li {:class (if (= @current-tab nil) "active")}
                               ;    [:a {:on-click #(swap-tab nil)}
                               ;     (:tool @step-data)]]
                               ;   [:li {:class (if (= @current-tab "data") "active")}
                               ;    [:a {:data-target "test"
                               ;         :on-click #(swap-tab "data")}
                               ;     "Data"]]]]

                               ;(println "step data" @step-data)

                               [:div.body
                                (if (:produced @step-data)
                                  [savetodrawer/main (select-keys @step-data [:_id :extra :produced])]
                                  ;[:div.btn.btn-primary.btn-circle.btn-lg.offset
                                  ; {:on-click #(re-frame/dispatch [:save-research _id])}
                                  ; [:svg.icon.molecule.out [:use {:xlinkHref "#leftturn"}]]]
                                  )
                                [:div {:className (if (= @current-tab "data") "hide")}
                                 [step (dissoc @step-data :saver :produced)]]
                                [:div {:className (if (= @current-tab nil) "hide")}
                                 (json-html/edn->hiccup @step-data)]
                                (if (:loading? @step-data) [tool-dimmer])]])})))

(defn steps-dashboard
  "Create a dashboard with a tool inside. The dashboard includes common
  functionality such as data tabs, notes, etc."
  [ids]
  (let [_ nil]
    (reagent/create-class
      {:display-name   "dashboard"
       :reagent-render (fn [ids]
                         [:div.step-container
                          [:div.body.dashboard
                           (for [rows (partition-all 3 ids)]
                             ^{:key (str "step-col" id)}
                             (for [id rows]
                               ^{:key (str "step-row" id)} [:div.cell ^{:key (str "step-container" id)} [step-container id true]]
                               )
                             )]])})))

(defn cont []
  (let [project      (re-frame/subscribe [:active-project])
        network      (re-frame/subscribe [:active-network])
        global-cache (re-frame/subscribe [:global-cache])]
    (fn [step-data]
      (let [location [:projects @project :networks @network :nodes (:_id step-data)]
            comms    {:has-something (partial api/has-something location)
                      :save-state    (partial api/save-state location)
                      :update-state    (partial api/update-state location)
                      :save-cache    (partial api/save-cache location)}
            tool     (-> bluegenes.tools
                         (aget (:tool step-data))
                         (aget "core")
                         (aget "main"))]
        [:div.step-container
         [savetodrawer/main step-data]
         [:div.body
          [tool (assoc step-data :api comms
                                 :global-cache @global-cache)]]]))))

(defn input-filter []
  (fn [data]
    [:ul.nav.nav-pills
     (for [i (:Gene (:decon data))]
       [:li
        {:class    (if (= (:path i) (:filter data)) "active")
         :on-click (fn []
                     (re-frame/dispatch [:set-input-filter (:_id data) (:path i)]))}
        [:a (str (:path i))]])

     ;[:svg {:width  100
     ;       :height 100}
     ; [:line {:x1    50 :y1 0
     ;         :x2    50 :y2 100
     ;         :style {:stroke       "#2196F3"
     ;                 :stroke-width 3}}]
     ; [:g {:transform "translate(50,50)"}
     ;  [:circle {:r     30
     ;            :style {:stroke       "#2196F3"
     ;                    :fill         "white"
     ;                    :stroke-width 2.5}}]
     ;  [:text {:text-anchor "middle"
     ;          :style       {:alignment-baseline "middle"}}
     ;   [:tspan 459]
     ;   [:tspan (-> data :input :data :type)]]]]


     ;(for [[class queries] (:decon data)]
     ;  [:span
     ;   ;[:span (str class)]
     ;   (into [:span]
     ;         (map (fn [query]
     ;                [:span {:on-click (fn []
     ;                                    (println "x" query))} (str (:path query))]) queries))])

     ]))

(defn previous-steps
  "Iterate through the history's structure and create step containers for
  single tools (keyword) or steps dashboards for grouped tools (vector)."
  []
  (let [step-path (re-frame/subscribe [:step-path])
        steps     (re-frame/subscribe [:steps])]
    (fn []
      (into [:div.prevsteps
             [whatnext/main]]
            (-> (map (fn [id]
                       [:div.abc
                        [:div.row
                         [:div.col-md-8 [cont (get @steps id)]]
                         [:div.col-md-4 [lighttable/main (:output (get @steps id))]]]
                        [input-filter (get @steps id)]]) (reverse @step-path)))

            ))))
(defn tabs []
  (let [networks       (subscribe [:networks])
        active-network (subscribe [:active-network])]
    (fn []
      [:div.tabber
       [:ul.nav.nav-tabs
        (for [[id details] @networks]
          (doall
            [:li {:on-click #(re-frame/dispatch [:set-timeline-panel :timeline-panel
                                                 "project1" (:slug details)])
                  :class    (if (= @active-network id) "active")}
             [:a (:label details)]]))
        [:li [:div.btn.btn-primary
              {:on-click #(dispatch [:new-network])} "New"]]]])))


(defn history-details []
  "Not used as of yet."
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
     [:h2 (:name @history)]
     [:h4 (:description @history)]]))

(defn saved-data-view []
  (let [active-project (re-frame/subscribe [:active-project])
        active-data    (re-frame/subscribe [:active-data])]
    [:div.timeline-container
     [drawer/main]
     [:div.stretchme
      [tabs]
      [:div.prevsteps
       [whatnext/main]
       [:div.step-container
        [viewtable/main {:state {:service (:service (:payload @active-data))
                                 :data    {:payload (viewtable/normalize-input (:payload @active-data))}}}]]]]]))

(defn operations []
  [:div.timeline-container
   [drawer/main]
   [:div.prevsteps
    [:div.step-container
     [:div.body
      [query-operations/main]]]]])


(defn main-view []
  [:div.timeline-container
   [drawer/main]

   [:div.stretchme
    [tabs]
    [previous-steps]]])
