(ns bluegenes.components.queryoperations.view
  (:require [re-frame.core :as re-frame]
            [bluegenes.components.queryoperations.handlers]
            [bluegenes.components.queryoperations.subs]
            [bluegenes.components.venn.view :as venn]))

(defn dropdown-research [position]
  (let [saved-research (re-frame/subscribe [:saved-research])]
    (fn [position]
     [:div
      [:div.btn-group
       [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
         [:span "Choose " [:span.caret]]]
       [:ul.dropdown-menu
        (for [[id details]  @saved-research]
          (do
            [:li
             {:on-click (fn [] (re-frame/dispatch [:set-qop position id]))}
             [:a (:label details)]]))]]])))

(defn selection-details [position]
  (let [representing (re-frame/subscribe [(keyword (str "qop-" position))])]
    [:p (str (:label @representing))]))

(defn operations []
  (let [op (re-frame/subscribe [:qop-op])]
    (fn []
     [:p @op])))

(defn doit []
  (fn []
    [:div.btn.btn-success
     {:on-click (fn [] (re-frame/dispatch [:run-qop]))} "DO THIS"]))

(defn main []
  (fn []
    [:div
     [:h1 "Query Operations"]
     [:div.query-operations-container
      [:div.child
       [dropdown-research 1]
       [selection-details 1]]
      [:div.child
       [venn/main]]
      [:div.child
       [dropdown-research 2]
       [selection-details 2]]]
     [:div.centered
      [operations] [doit]]]))