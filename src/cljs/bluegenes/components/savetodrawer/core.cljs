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
      (for [[path details] (:extra payload)]
        [:li
         {:on-click (fn []

                      (let [saveme {:service (:service (:produced payload))
                                    :data {:format "query"
                                           :type "Gene"
                                           :payload details}}]

                        (re-frame/dispatch [:save-research (:_id payload) saveme])))}
         [:a (str (:display-name details) "s")]])
      ;(for [{:keys [display-name query count] :as to-save} (:saver payload)]
      ;  [:li
      ;   ;{:on-click #(println to-save)}
      ;   {:on-click (fn []
      ;                (println "to-save" to-save)
      ;                (let [saveme {:service (:service to-save)
      ;                              :data {:format "query"
      ;                                     :type "Gene"
      ;                                     :payload query}}]
      ;                  (println "saveme" saveme)
      ;                  (re-frame/dispatch [:save-research (:_id payload) saveme])))}
      ;   [:a
      ;    [:span.badge.active count]
      ;    [:span (str " " display-name)]]])

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
    [:div.btn.btn-success
     ;{:on-click #(re-frame/dispatch [:save-research])}
     [:i.fa.fa-floppy-o]
     [:span (str " Save List")]]))

(defn main []
  (fn [output]
    (let [format (-> output :data :format)]
      [:div
       (cond
         (= "ids" format) [ids-saver output]
         (= "query" format) [query-saver output]
         (= "list" format) [list-saver output])])))
