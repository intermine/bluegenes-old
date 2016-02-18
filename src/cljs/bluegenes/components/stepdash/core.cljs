(ns bluegenes.components.stepdash.core
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]
            [bluegenes.toolmap :as toolmap]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.tools.viewtable.core :as viewtable]))


(def data-categories ["Genes" "Proteins" "Homology" "Pathways" "Annotation" ])

(defn long-content []
  (fn []
    (into [:ul] (map (fn [] [:li (str (rand-int 100))]) (range 20)))))







(def menu-state (reagent/atom {:open false
                               :category nil}))




(defn atom-viewer []
  (fn []
    [:h1 (str @menu-state)]))

(defn mock-category []
  [:div.dash-col
   [:h3 "Templates"]
   [:span "Exons"]
   [:span "Introns"]])

(defn dash []
  (fn []
    [:div.dash
     [mock-category]
    ;  [:h1 "sup. ¯\\_(ツ)_/¯"]
    ;  [:h4 "from category " (str (:category @menu-state))]
     ]))

(defn category [data]
  (fn []
    [:div.category
     {:class (if (= data (:category @menu-state)) "focused")
      :on-mouse-enter (fn [e] (swap! menu-state assoc :category data))}
     (str data)]))

(defn categories [& [{:keys [fixed items]}]]
  (fn []
    [:div.categories {:class (if fixed "fixed")}
     (map (fn [d] [category d]) items)
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
      [step-dash])))
