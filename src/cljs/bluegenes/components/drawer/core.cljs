(ns bluegenes.components.drawer.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn new []
  (fn []
    [:div.heading
     ;[:span.count [:div.btn.btn-circle.btn-default.btn-lg
     ;              [:svg.icon.molecule
     ;               [:use {:xlinkHref "#molecule"}]]
     ;              ]]
     [:div.new [:div.btn.btn-default "New Search"]]
     ]))

(defn item []
  (fn [{:keys [content count]}]
    [:div.item
     [:span.fa-2x.ico [:svg.icon.molecule [:use {:xlinkHref "#molecule"}]]]
     [:span.grow content]
     [:span.count [:span.big count] [:span.right "Genes"]] ]))

(defn main []
  (fn []
    [:div.drawer
     [:div.heading [:h3 "Saved Research"]]
     [item {:content "Uploaded list of genes."
            :count 1449}]
     [item {:content "Genes associate with Alzheimer's pathways."
            :count 807}]
     [item {:content "Genes associated with Alzheimer's GWAs."
            :count 67}]
     [item {:content "My genes minus known Alzheimer's genes."
            :count 523}]
     [item {:content "Mouse homologues."
            :count 499}]
     [new]]))