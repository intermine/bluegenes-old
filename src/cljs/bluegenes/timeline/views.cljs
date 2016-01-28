(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            ; [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [cljs.contrib.pprint :refer [pprint]]))

(defn append-state [tool data]
  (re-frame/dispatch [:append-state (keyword (:uuid tool)) data]))

(defn replace-state [tool data]
  (re-frame/dispatch [:replace-state (:uuid tool) data]))

(defn has-something [tool data]
  (re-frame/dispatch [:has-something (keyword (:uuid tool)) data]))

(defn somedimmer []
  [:div.dimmer
   [:div.message
    [:div.loader]]])

(defn step []
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
     {:reagent-render (fn [step-data]
                        (.debug js/console "Loading Step:" step-data)
                        (let [_ nil]
                          [:div.step-container
                           [:div.step-inner
                            ;  [:h4 (:description step-data)]
                            [:div.toolbar
                             [:ul.nav.nav-tabs
                              [:li {:class (if (= @current-tab nil) "active")} [:a {:on-click #(swap-tab nil)} (:tool step-data)]]
                              [:li {:class (if (= @current-tab "data") "active")} [:a {:data-target "test"
                                                                                       :on-click    #(swap-tab "data")} "Data"]]]]
                            [:div.body
                             (cond
                               (= nil @current-tab)
                               (do
                                 [(-> bluegenes.tools (aget (:tool step-data)) (aget "core") (aget "main"))
                                  step-data {:append-state (partial append-state step-data)
                                             :replace-state (partial replace-state step-data)
                                             :has-something (partial has-something step-data)}])
                               (= "data" @current-tab)
                               (json-html/edn->hiccup step-data))
                            ;  [somedimmer]
                             ]
                            ]]))})))

(defn step-tree [steps]
  "Serialize the steps of a history."
  (let [all-notifiers (remove nil? (map (fn [[step value]] (:notify value)) steps))
        [starting-point] (utils/diff all-notifiers (keys steps))]
    (loop [id starting-point
           step-vec []]
      (if-not (contains? (id steps) :notify)
        (do
          (conj step-vec (id steps)))
        (recur (:notify (id steps)) (conj step-vec (id steps)))))))

(defn previous-steps []
  (let [steps-reaction (re-frame/subscribe [:steps])
        mines (re-frame/subscribe [:mines])]
    (let [steps @steps-reaction]
      (if (nil? steps)
        [:h1 "New history"]
        (into [:div] (for [s (reverse (step-tree steps))]
                       (do ^{:key (:uuid s)} [step (assoc s :mines @mines) nil])))))))

(defn history-details []
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
      [:div.step-inner
       [:h2 (:name @history)]
       [:h4 (:description @history)]]]))

(defn main-view []
    [:div
     [nextsteps/main]
     [previous-steps]])
