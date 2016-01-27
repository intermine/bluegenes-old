(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.nextsteps.core :as nextsteps]
            [bluegenes.utils :as utils]
            [cljs.contrib.pprint :refer [pprint]]))

(defn append-state [tool data]
  (re-frame/dispatch [:append-state (keyword (:uuid tool)) data]))

(defn replace-state [tool data]
  (println "replacing state on tool" (:uuid tool))
  (println "with data" data)
  (re-frame/dispatch [:replace-state (:uuid tool) data]))

(defn has-something [tool data]
  (re-frame/dispatch [:has-something (keyword (:uuid tool)) data]))

(defn step []
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
      {:reagent-render (fn [step-data]
        (println "looking for tool"  (:tool step-data))
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
                                (json-html/edn->hiccup step-data))]]]))})))

(defn step-tree [steps]
  "Serialize the steps of a history."
  (let [all-notifiers (remove nil? (map (fn [[step value]] (:notify value)) steps))
        [starting-point] (utils/diff all-notifiers (keys steps))]
    (loop [id starting-point
           step-vec []]
      ; (println "starting point is" starting-point)
      (if-not (contains? (id steps) :notify)
        (do
          ; (println "final tree is" (conj step-vec (id steps)))
          (conj step-vec (id steps)))
        (recur (:notify (id steps)) (conj step-vec (id steps)))))))

(defn previous-steps []
  (let [steps (re-frame/subscribe [:steps])
        mines (re-frame/subscribe [:mines])]
    ; (into [:div] (for [s (step-tree @steps) :when (contains? s :input)]
    (println "allsteps")
    (println (pprint (step-tree @steps)))
    (into [:div] (for [s (reverse (step-tree @steps))]
                   (do
                     (.debug js/console "Loading step" (clj->js s))
                     ^{:key (:uuid s)} [step (assoc s :mines @mines) nil])))))

(defn history-details []
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
      [:div.step-inner
       [:h2 (:name @history)]
       [:h4 (:description @history)]]]))

(defn main-view []
    [:div
     [nextsteps/main]
     [previous-steps]
     [dimmer/main]])
