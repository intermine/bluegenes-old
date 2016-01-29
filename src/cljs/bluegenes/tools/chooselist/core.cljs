(ns bluegenes.tools.chooselist.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]))

(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(def lists (reagent/atom nil))

(defn get-lists []
  (-> flymine .fetchLists (.then (fn [im-lists]
                                   (reset! lists im-lists)))))
(defn list-row []
  (reagent/create-class
    {:reagent-render (fn [list responders]
                        [:div
                         [:span.badge (.-size list)]
                         [:span {:on-click (fn []
                                             ((:append-state responders) {:chose (.-name list)})
                                             ((:has-something responders) {:service {:root "www.flymine.org/query"}
                                                                           :data {:format "list"
                                                                                  :type (.-type list)
                                                                                  :name (.-name list)}}))}
                          (.-name list)]
                         [:span.badge.pull-right (.-type list)]])}))

(defn ^:export main [step-data responders]
  (let [nothing nil]
    (reagent/create-class
      {:reagent-render (fn [step-data]
                         [:div
                          ; [:h2 (:description step-data) ]
                          [:div (for [l @lists]
                                  ^{:key (.-name l)} [list-row l responders])]]
                         )
       :component-did-mount get-lists })))
