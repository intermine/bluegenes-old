(ns bluegenes.components.nextsteps.core
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
          ^{:key name} [tool @available-data]
          name)]])))

          ; (.tooltip (js/$ (reagent/dom-node this))
          ;           #js{:title @data
          ;               :data-placement "bottom"
          ;               :html true})

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

(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [:div.next-steps
       [:div.next-steps-title "Next Steps"]
       [:div.tool-card-container
        (for [tool (filter-available-tools (:type (:data @available-data)) )]
          (let [[id] tool]
            ^{:key id} [tool-card tool]  ))
        [data-popover]]
       [:div.clear-fix]])))
