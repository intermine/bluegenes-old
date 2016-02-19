(ns bluegenes.components.stepdash.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]
            [bluegenes.toolmap :as toolmap]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.tools.viewtable.core :as viewtable]))


(def data-categories ["Genes" "Proteins" "Homology" "Pathways" "Annotation" ])

(def menu-state (reagent/atom {:open false
                               :category nil}))

(defn filter-available-tools [datatype]
  (filter (fn [[tool-name tool-data]]
            (if (= (-> tool-data :accepts :type) datatype)
              true
              false)) (seq toolmap/tools)))


(defn atom-viewer []
  (fn []
    [:h1 (str @menu-state)]))

(defn mock-category []
  [:div.dash-col
   [:h3 "Templates"]
   [:span "Exons"]
   [:span "Introns"]])


(defn preview-container [name props]
  (println "inspecting" name)
  (let [available-data (re-frame/subscribe [:available-data])
        tool (-> bluegenes.tools
                 (aget name)
                 (aget "core")
                 (aget "preview"))]

    (fn []
      (println "Loading preview")
      [:div.dash-col
      ;  {:on-click (fn [] (next-step-handler name))}
       [:div.title (:title props)]
       [:div.body
        (if-not (nil? tool)
          ^{:key name} [tool (merge @available-data {:category (:category @menu-state)})]
          name)]])))



(defn dash
  "Show all tools relevant to the current category."
  []
  (let [available-data (re-frame/subscribe [:available-data])
        category (:category @menu-state)]
    (fn []
      [:div.dash
       (for [tool (filter-available-tools (:type (:data @available-data)) )]
                (let [[id] tool]
                  (println "passing id" id)
                  [preview-container id tool] ))
      ;  [:h4 "from category " (str (:category @menu-state))]
       ])))

(defn category [data]
  (fn []
    [:div.category
     {:class (if (= data (:category @menu-state)) "focused")
      :on-mouse-enter (fn [e] (swap! menu-state assoc :category data))}
     (str data)]))

(defn categories [& [{:keys [fixed items]}]]
  (fn []
    [:div.categories {:class (if fixed "fixed")}
     (map (fn [d] ^{:key d} [category d]) items)
    ;  [atom-viewer]
     ]))

(defn step-dash []
  (let [state (reagent/atom {})]
    (reagent/create-class
     {:component-did-mount (fn [this]
                             (let [el (reagent/dom-node this)]
                               (dommy/listen! el
                                              :mouseenter
                                              #(dommy/add-class! el "focus"))
                               (dommy/listen! el
                                              :mouseleave
                                              #(dommy/remove-class! el "focus"))))
      :reagent-render (fn []
                        [:div.step-all
                         [categories {:items data-categories}]
                         [dash]])})))


(defn main []
  (let [available-data (re-frame/subscribe [:available-data])]
    (fn []
      ; (println "HELLO" (filter-available-tools (:type (:data @available-data))))
      ; (doall (for [tool (filter-available-tools (:type (:data @available-data)) )]
      ;   (let [[id] tool]
      ;     (println "CAN RUN" tool) )))
      [step-dash])))
