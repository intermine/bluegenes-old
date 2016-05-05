(ns bluegenes.tools.templatechooser.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [clojure.string :as str]
            [bluegenes.tools.templatechooser.helpers :as h]
            [bluegenes.utils.imcljs :as imcljs]))

(enable-console-print!)

;(defn process-query [query model payload]
;  (println "query" (update-in query [:where]
;                              (fn [constraints]
;                                (h/replace-input-constraint model constraints "Gene" )
;                                ))))


(defn template []
  (fn [{:keys [step-data template]}]
    (fn []
      (let [[id query] template
            has-something (-> step-data :api :has-something)]
        [:a.list-group-item
         {:on-click (fn []
                      (println "query" has-something)
                      (has-something {:service {:root "www.flymine.org/query"}
                                      :data {:format "query"
                                             :type "Gene"
                                             :payload query}
                                      :shortcut "viewtable"})
                      )}
         [:h4.list-group-item-heading
          (last (clojure.string/split (:title query) "-->"))]
         [:p.list-group-item-text (:description query)]]))))


(defn templates [step-data]
  (let [templates (re-frame/subscribe [:templates])
        models    (re-frame/subscribe [:models])]
    (fn []
      (let [mine-templates       (:flymine @templates)
            mine-model           (:flymine @models)
            runnable-templates   (into {}  (h/runnable mine-model mine-templates "Gene"))
            replaced-constraints (into {}  (map (fn [[id query]]
                                         [id (h/replace-input-constraints
                                               mine-model
                                               query
                                               "Gene"
                                               (-> step-data :upstream-data :data :payload))])
                                       runnable-templates))
            ]

        ;(println "replaced input constrAINTSINSDGISDNFG" replaced-constraints)
        ;(println "DISEASE--" (:Gene_disease replaced-constraints))
        ;(println "end-class" (imcljs/trim-path-to-class mine-model
        ;                                       "Disease.genes.homologues.homologuess"))
        [:div.list-group
         (for [t replaced-constraints]
           [template {:step-data step-data
                      :template t}])]

        ))))

(defn upstream-data []
  (fn [data]
    [:div (str data)]))

(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [step-data]
                       [:div
                        [upstream-data (-> step-data :upstream-data :data :payload count)]
                        [templates step-data]])}))

