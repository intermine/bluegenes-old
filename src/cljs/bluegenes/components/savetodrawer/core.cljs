(ns bluegenes.components.savetodrawer.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.utils.imcljs :as im]
            [cljs.core.async :as async :refer [put! chan <! >! timeout close!]]))

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

;[trimmed-paths (distinct (reduce (fn [total next]
;                                   (conj total (im/trim-path-to-class model next))) [] (:select test-query)))]

(defn query-saver []
  (fn [payload]
    [:div.btn-group
     [:div.btn.btn-success.dropdown-toggle {:data-toggle "dropdown"}
      [:i.fa.fa-floppy-o] [:span " Save Data " [:span.caret]]]
     [:ul.dropdown-menu.savetodrawer
      (for [{:keys [display-name count]} payload]
        [:li [:a
              [:span.badge.active count]
              [:span (str " " display-name)]]])]]))

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
    [:p "list saver"]))

(defn main []
  (fn [step-data]
    (let [{:keys [format type payload] :as out} (-> step-data :produced :data)]
      [:div
       ;[:h4 "SAVE TO DRAWER"]
       (cond
         (= "ids" format) [ids-saver step-data]
         (= "query" format) [query-saver (:saver step-data)]
         (= "list" format) [list-saver payload])])))

;(defn main []
;  (fn [step-data]
;    [:div
;     [:h4 "SAVE TO DRAWER"]
;     [:p (str "test")]]))