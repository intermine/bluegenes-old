(ns bluegenes.tools.runtemplate.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def state (reagent/atom {}))

(defn filter-for-type [template]
  (let [wheres (get-in template [:where])]
    (if (true? (some #(= (:path %) "Gene") wheres))
      template)))

(defn fetch-templates []
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        templates (-> mine .fetchTemplates)]
    (-> templates (.then (fn [r]
                           (let [results (js->clj r :keywordize-keys true)
                                 chopped (into [] (for [k (keys results)]
                                                    (assoc (get results k) :id k)))]
                             (swap! state assoc :templates results)
                             (swap! state assoc :filtered (doall (filter filter-for-type chopped)))
                             ))))))


(defn get-id-query [service list]
  {:from "Gene"
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"
            }]})

(defn constraint [cons]
  [:div.form-group
   [:label (:path cons)]
   [:input.form-control {:type "text"
                         :value (:value cons)}]])

(defn replace-constraints [cons ids]
  (if (= (:path cons) "Gene")
    (assoc cons :value ids :path "Gene.id")
    cons))

(defn button-runner []
  [:button.btn.disabled {:disabled "disabled"} "Run"])

(defn selected-template [has]
  (println "HAS" has)
  (fn []
    (let [templates (:templates @state)
          selected (:selected @state)
          thekey (keyword (str selected))
          template (thekey templates)]
      [:div
       [:span (str template)]
       [:h2 (:name template)]
       [:h4 (:description template)]
       [:div (doall (for [cons (map #(replace-constraints % (-> has :input :data :values) ) (:where template))] [constraint cons]))]
       [:span (str (:where template))]])))

(defn template-dropdown []
  ; Holds a drop down of lists in an item and populates the selected atom with
  [:select.form-control {:on-change (fn [e]
                                      (swap! state assoc :selected (-> e .-target .-value)))}
   (for [item (:filtered @state)]
     ^{:key (:name item)} [:option
                           {:value (:id item)}
                           (:name item)])])

(defn ^:export main [data]
  (fetch-templates)
  (fn [data]
    (println "Template tool is rendering.")
    [:div
     [template-dropdown]
     [selected-template data]
     [button-runner]
     ]))
