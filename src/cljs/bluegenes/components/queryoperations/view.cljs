(ns bluegenes.components.queryoperations.view
  (:require [re-frame.core :as re-frame]
            [bluegenes.components.queryoperations.handlers]
            [bluegenes.components.queryoperations.subs]
            [reagent.core :as reagent]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn dropdown-research [position]
  (let [saved-research (re-frame/subscribe [:saved-research])
        representing (re-frame/subscribe [(keyword (str "qop-" position))])]
    (fn [position]
      (println "REP" @representing)
     [:div
      [:h3 (str "Position " @representing)]
      [:div.btn-group
       [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
        [:i.fa.fa-floppy-o] [:span (str (:label @representing)) [:span.caret]]]
       [:ul.dropdown-menu
        (for [[id details]  @saved-research]
          (do
            [:li
             {:on-click (fn []
                          (println "ID" id)
                          (re-frame/dispatch [:set-qop position id]))}
             [:a (:label details)]]))]]])))

(defn operations []
  (let [op (re-frame/subscribe [:qop-op])]
    (fn []
     [:div
      [:h3 (str "Action " @op)]
      [:div.btn-group
       [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
        [:i.fa.fa-floppy-o] [:span (str @op) [:span.caret]]]
       [:ul.dropdown-menu
        [:li
         {:on-click (fn [] (re-frame/dispatch [:set-qop-op "combine"]))}
         [:a "Combine"]]
        [:li
         {:on-click (fn [] (re-frame/dispatch [:set-qop-op "intersect"]))}
         [:a "Intersect"]]]]])))

(defn doit []
  (fn []
    [:div
     [:h3 "RUN"]
     [:div.btn.btn-success
      {:on-click (fn [] (re-frame/dispatch [:run-qop]))} "DO THIS"]]))

(defn available-research []
  (fn []
    [:div
     [dropdown-research 1]
     [dropdown-research 2]
     [operations]
     [doit]]))

(defn main []
  (fn []
    [:div
     [:h1 "Query Operations"]
     [available-research]]))

