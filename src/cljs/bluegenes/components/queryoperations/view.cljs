(ns bluegenes.components.queryoperations.view
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.components.queryoperations.handlers]
            [bluegenes.components.queryoperations.subs]
            [bluegenes.components.venn.view :as venn]))

(defn dropdown-research [position]
  (let [saved-research (subscribe [:saved-research])
        lists (subscribe [:lists])]
    (fn [position]
     [:div
      [:div.btn-group
       [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
         [:span (str "Choose " (:label @saved-research)) [:span.caret]]]
       [:div.dropdown-menu.scrollable-menu
        [:h4 "Saved Data"]
        [:ul.list-unstyled
         (for [[id details]  @saved-research]
           (do
             [:li
              {:on-click (fn [] (re-frame/dispatch [:set-qop position id :saved-data]))}
              [:a (:label details)]]))]
        [:h4 "Lists"]
        [:ul.list-unstyled
         (for [details (:flymine @lists)]
           (do
             [:li
              {:on-click (fn [] (re-frame/dispatch [:set-qop position (:name details) :list :flymine]))}
              [:a (:title details)]]))]]]])))

(defn dropdown-research-path [position]
  (let [selected (re-frame/subscribe [(keyword (str "qop-" position))])]
    (fn []
      [:div
       [:div.btn-group
        [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
         [:span "Choose " [:span.caret]]]
        [:div.dropdown-menu.scrollable-menu
         [:h4 "Saved Data"]
         [:ul.list-unstyled
          (for [[class queries] (:deconstructed @selected)]
            [:li [:ul (for [q queries]
                        (do
                          [:li
                           {:on-click (fn [] (re-frame/dispatch [:update-qop-query position (:query q)]))}
                           [:a (:path q)]]))]])]]]])))

(defn selection-details [position]
  (let [representing (re-frame/subscribe [(keyword (str "qop-" position))])]
    [:p (str @representing)]))

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
       [dropdown-research-path 1]
       [selection-details 1]]
      [:div.child
       [venn/main]]
      [:div.child
       [dropdown-research 2]
       [dropdown-research-path 2]
       [selection-details 2]]]
     [:div.centered
      [operations] [doit]]]))