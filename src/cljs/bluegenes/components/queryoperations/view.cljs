(ns bluegenes.components.queryoperations.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn dropdown-research []
  (let [saved-research (re-frame/subscribe [:saved-research])]
    (fn []
     [:div
      [:h3 "Target One"]

      [:div.btn-group
       [:div.btn.btn-primary.dropdown-toggle {:data-toggle "dropdown"}
        [:i.fa.fa-floppy-o] [:span " Target One " [:span.caret]]]
       [:ul.dropdown-menu
        (for [[id details]  @saved-research]
          (do
            [:li [:a (:label details)]]))]]



      ;[:div.dropdown
      ; [:div.btn.btn-success.dropdown-toggle {:data-toggle "dropdown"} "Target"
      ;  (into [:ul.dropdown-menu]
      ;        (map (fn [[id details]]
      ;               [:li [:a (:label details)]]) @saved-research))]]
      ])))

(defn available-research []
  (fn []
    [:div
     [dropdown-research]]))

(defn main []
  (fn []
    [:div
     [:h1 "Query Operations"]
     [available-research]]))

