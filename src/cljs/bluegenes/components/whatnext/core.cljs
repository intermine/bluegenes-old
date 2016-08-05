(ns bluegenes.components.whatnext.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]
            [bluegenes.toolmap :as toolmap]
            [bluegenes.tools.viewtable.core :as viewtable]))

(defn filter-available-tools [datatype]
  (filter (fn [[tool-name tool-data]]
            (if (= (-> tool-data :accepts :type) datatype)
              true
              false)) (seq toolmap/tools)))

(defn next-step-handler [name]
  (re-frame/dispatch [:add-step name]))

(defn tool-card [[name props]]
  (let [available-data (re-frame/subscribe [:available-data])
        tool           (-> bluegenes.tools
                           (aget name)
                           (aget "core")
                           (aget "preview"))]

    (fn []
      [:div.tool-card
       {:on-click (fn [] (next-step-handler name))}
       [:div.title (:title props)]
       [:div.body
        (if-not (nil? tool)
          ^{:key name} [tool @available-data]
          name)]])))

(defn data-popover []
  (let [available-data (re-frame/subscribe [:available-data])]
    (reagent/create-class
      {:component-did-update
       (fn [this]
         (let [html (json-html/edn->html @available-data)]
           (-> (js/$ (reagent/dom-node this))
               (.tooltip #js{:title html
                             :placement "bottom"
                             :html true})
               (.attr "data-original-title" html)
               (.tooltip "fixTitle"))))
       :reagent-render
       (fn []
         (str @available-data)
         [:div.btn.btn-raised
          ; {:style {:margin-left "auto"}}
          "Debug"])})))

{:_id :k1
 :tool "templatechooser"
 :state []}

(defn tabletool []
  (fn []
    [:button.next-tool
     {:on-click #(re-frame/dispatch [:add-step "viewtable" {}])}
     [:svg.icon.icon-table [:use {:xlinkHref "#icon-table"}]]
     "View All Results"]))

(defn templatetool []
  (fn []
    [:button.next-tool
     {:on-click #(re-frame/dispatch [:add-step "templatechooser" {}])}
     [:svg.icon.icon-template [:use {:xlinkHref "#icon-search"}]]
     "Run Template"]))

(defn enrichtool []
  (fn []
    [:button.next-tool
     {:on-click #(re-frame/dispatch [:add-step "enrichment" {:widget "pathway_enrichment"
                                                             :title "Pathway Enrichment"}])}
     [:svg.icon.icon-barchart [:use {:xlinkHref "#icon-bar-chart"}]]
     "Run Enrichment"]))

(defn mergetool []
  (fn []
    [:button.next-tool
     {:on-click #(re-frame/dispatch [:add-step "datamerger" {}])}
     [:svg.icon.icon-table [:use {:xlinkHref "#icon-venn"}]]
     "Merge Data"]))


;;;TODO: figure out if we can delete this or spruce it up? It doesn't work right now
(defn listtool []
  (fn []
    [:button.next-tool
     {:on-click #(re-frame/dispatch
                  [:add-step "dashboard" {:active 0
                                          :tools [{:tool "enrichment"
                                                   :state [{:widget "go_enrichment_for_gene"
                                                            :correction "Holms-Bonferroni"
                                                            :title "Gene Ontology Enrichment"}]}
                                                  {:tool "enrichment"
                                                   :state [{:widget "pathway_enrichment"
                                                            :title "Pathway Enrichment"}]}
                                                  {:tool "enrichment"
                                                   :state [{:widget "prot_dom_enrichment_for_gene"
                                                            :title "Protein Domain Enrichment"}]}
                                                  {:tool "enrichment"
                                                   :state [{:widget "prot_dom_enrichment_for_gene"
                                                            :title "Protein Domain Enrichment"}]}
                                                  {:tool "enrichment"
                                                   :state [{:widget "prot_dom_enrichment_for_gene"
                                                            :title "Protein Domain Enrichment"}]}
                                                  {:tool "enrichment"
                                                   :state [{:widget "prot_dom_enrichment_for_gene"
                                                            :title "Protein Domain Enrichment"}]}
                                                  ]}])}
     "Analyze List"]))



(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [:div.next-step-chooser
       [:div "Tools:"]
        [templatetool]
        [enrichtool]
        [mergetool]
        [tabletool]

])))
