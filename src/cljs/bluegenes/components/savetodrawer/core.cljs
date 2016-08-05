(ns bluegenes.components.savetodrawer.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.utils.imcljs :as im]
            [cljs.core.async :as async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def test-query {:from "Gene"
                 :select ["Gene.symbol"
                          "Gene.secondaryIdentifier"
                          "Gene.goAnnotation.ontologyTerm.identifier"
                          "Gene.goAnnotation.ontologyTerm.name"
                          "Gene.goAnnotation.evidence.code.code"
                          "Gene.goAnnotation.ontologyTerm.namespace"
                          "Gene.goAnnotation.qualifier"]
                 :where [{:path "Gene"
                          :op "LOOKUP"
                          :value "Notch"
                          :extraValue ""
                          :code "A"
                          :editable true
                          :switched "LOCKED"
                          :switchable false}]})


(defn query-saver []
  (fn [payload]
    ;(.log js/console "query saver payload" payload)
    [:div.btn-group
     [:div.btn.btn-success.dropdown-toggle {:data-toggle "dropdown"}
      [:i.fa.fa-floppy-o] [:span " Save Data " [:span.caret]]]
     [:ul.dropdown-menu.savetodrawer
      (for [[view details] (:export payload)]
        (do
          [:li
           {:on-click #(re-frame/dispatch [:save-research
                                           (:_id payload)
                                           (assoc-in (:output payload)
                                                     [:data :payload] (:query details))])}
          [:a (str (:display-name details) "s")]]))
      ]]))

(defn ids-saver []
  (fn [{:keys [data saver _id] :as step-info}]
    [:div.btn.btn-success
     {:on-click #(re-frame/dispatch [:save-research _id])}
     [:i.fa.fa-floppy-o]
     [:span (str " Save "
                 (count (-> saver first :data :payload))
                 " "
                 (-> saver first :data :type)
                 "s")]]))

(defn list-saver []
  (fn [payload]
    ;(println "list saver sees payload" (:output payload))
    [:div.next-tools
      [:button.btn.btn-success
        {:on-click #(re-frame/dispatch [:save-research (:_id payload) (:output payload)])}
        [:i.fa.fa-floppy-o]
        [:span (str " Save to cart")]]
     ]))

(defn main []
  (fn [step-data]
    (let [format (-> step-data :output :data :format)]
      [:div
       (cond
         (= "ids" format) [ids-saver step-data]
         (= "query" format) [list-saver step-data]
         (= "list" format) [list-saver step-data])])))
