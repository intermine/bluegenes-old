(ns bluegenes.components.stepdash.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]
            [bluegenes.toolmap :as toolmap]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.tools.viewtable.core :as viewtable]))


(def data-categories ["Genes" "Proteins" "Interactions" "Gene Ontology" "Literature" "Homology" "Pathways"])

(def menu-state (reagent/atom {:open false
                               :category nil}))

(defn filter-available-tools [datatype]
  (filter (fn [[tool-name tool-data]]
            (if (= (-> tool-data :accepts :type) datatype)
              true
              false)) toolmap/tools))

(defn next-step-handler [name]
  (re-frame/dispatch [:add-step name]))

(defn atom-viewer []
  (fn []
    [:h1 (str @menu-state)]))

(defn mock-category []
  [:div.dash-col
   [:h3 "Templates"]
   [:span "Exons"]
   [:span "Introns"]])


(defn preview-container [name props]
  (let [available-data (re-frame/subscribe [:available-data])
        tool (-> bluegenes.tools
                 (aget name)
                 (aget "core")
                 (aget "preview"))]

    (fn []
      [:div.dash-col
       {:on-click (fn [] (next-step-handler name))}
       [:div.title (:title props)]
       [:div.body
        (if-not (nil? tool)
          ^{:key name} [tool (merge @available-data {:category (:category @menu-state)})])]])))

(defn dash
  "Show all tools relevant to the current category."
  [visible]
  (let [available-data (re-frame/subscribe [:available-data])
        category (:category @menu-state)]
    (fn [visible]
      [:div.dash {:class (if-not (true? visible) "hidden")}
       (for [tool (filter-available-tools (:type (:data @available-data)) )]
                (let [[id] tool]
                ^{:key id} [preview-container id tool]))])))

(defn stringify-class [& args]
  (clojure.string/join " " args))

(defn category [data]
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [:div.category
       {:class (stringify-class
                (if (nil? @available-data)
                  "disabled"
                  (if (= data (:category @menu-state)) "focused")))
        :on-mouse-enter (fn [e] (swap! menu-state assoc :category data))}
       (str data)])))

(defn categories [& [{:keys [fixed items]}]]
  (fn []
    [:div.categories {:class (if fixed "fixed")}
     (map (fn [d] ^{:key d} [category d]) items)
    ;  [atom-viewer]
     ]))

(defn step-dash []
  (let [available-data (re-frame/subscribe [:available-data])
        state (reagent/atom {})]
    (reagent/create-class
     {:component-did-mount (fn [this]
                             (let [el (reagent/dom-node this)]
                               (dommy/listen! el
                                              :mouseenter
                                              #(if-not (nil? @available-data)
                                                 (swap! state assoc :visible true)))
                               (dommy/listen! el
                                              :mouseleave
                                              #(swap! state assoc :visible false))))
      :reagent-render (fn []
                        [:div.step-all
                         [categories {:items data-categories}]
                         [dash (:visible @state)]])})))


(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      [step-dash])))
