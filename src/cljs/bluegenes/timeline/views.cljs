(ns bluegenes.timeline.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [json-html.core :as json-html]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.nextsteps.core :as nextsteps]))

(defn settle [tool data]
  (re-frame/dispatch [:settle tool data]))

(defn append-state [tool data]
  (re-frame/dispatch [:append-state tool data]))

(defn has-something [tool data & settle]
  (re-frame/dispatch [:has-something tool data settle]))

(defn step []
  (let [current-tab (reagent/atom nil)
        swap-tab (fn [name] (reset! current-tab name))]
    (reagent/create-class
      {:reagent-render (fn [step-data]

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
                                              :has-something (partial has-something step-data)}])
                                (= "data" @current-tab)
                                (json-html/edn->hiccup step-data)
                                )]]]))})))

(defn settled-filter [step]
  (true? (:settled step)))

(defn not-settled-filter [step]
  (false? (:settled step)))

(defn previous-steps []
  (let [steps (re-frame/subscribe [:steps])
        mines (re-frame/subscribe [:mines])]
    (into [:div] (for [s (reverse @steps)
                       :when (contains? s :input)]
                   (do
                     (.debug js/console "Loading step" (clj->js s))
                     [step (assoc s :mines @mines) nil])))))

(defn history-details []
  (let [history (re-frame/subscribe [:history])]
    [:div.step-container
      [:div.step-inner
       [:h2 (:name @history)]
       [:h4 (:description @history)]]]))

(defn main-view []
  (let [steps (re-frame/subscribe [:steps])]
    [:div
     [nextsteps/main]
    ;  [history-details]
     [previous-steps]
     [dimmer/main]]))
