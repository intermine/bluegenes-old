(ns bluegenes.components.nextsteps.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
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
        tool (-> bluegenes.tools
                 (aget name)
                 (aget "core")
                 (aget "preview"))]

    (fn []
      [:div.tool-card
       {:on-click (fn [] (next-step-handler name))}
       [:div.title (:title props)]
       [:div.body
        (if-not (nil? tool)
          [tool @available-data]
          name)]])))

(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [:div.next-steps
       [:div.next-steps-title "Next Steps"]
       (into [:div.tool-card-container]
             (for [tool (filter-available-tools (:type (:data @available-data)) )]
               [tool-card tool]))
       [:div.clear-fix]
       (json-html/edn->hiccup @available-data)
       ])))
