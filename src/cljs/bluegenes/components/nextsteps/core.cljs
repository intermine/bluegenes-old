(ns bluegenes.components.nextsteps.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [bluegenes.toolmap :as toolmap]))

(defn filter-available-tools [datatype]
  (filter (fn [[tool-name tool-data]]
            (if (= (-> tool-data :accepts :format) datatype)
              true
              false)) (seq toolmap/toolmap)))

(defn next-step-handler [name]
  (println "dipatching name" name)
  (re-frame/dispatch [:create-next-step]))

(defn tool-card [[name props]]
  [:div.tool-card
   {:on-click (fn [] (next-step-handler name))}
   [:div.title name]])

(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [:div.next-steps
       (into [:div.tool-card-container]
             (for [tool (filter-available-tools "list" )]
               [tool-card tool]))
       [:div.clear-fix]
       (json-html/edn->hiccup @available-data)
       ])))
