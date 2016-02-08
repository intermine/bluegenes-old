(ns bluegenes.tools.chooselist.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]))

(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(def lists (reagent/atom nil))

(defn get-lists []
  (-> flymine .fetchLists (.then (fn [im-lists]
                                   (reset! lists im-lists)))))

(defn is-selected [list state]
  "Returns true when a list name matches the most recent state (user chosen) list name"
  (= (.-name list) (:chose state)))

(defn list-row []
  "Generates a single list row with  counts and list type."
  (reagent/create-class
    {:reagent-render (fn [list responders state]
      [:tr {:on-click (fn []
                         ((:append-state responders) {:chose (.-name list)})
                         ((:has-something responders) {:service {:root "www.flymine.org/query"}
                                                       :data {:format "list"
                                                              :type (.-type list)
                                                              :name (.-name list)}}))
            :class (if (is-selected list state)
                     "selected")}
        [:td {:class (str "type-" (.-type list) " list-type")} (.-type list)]
        [:td {:class "count"} (.-size list)]
        [:td {:class "list-name"} (.-name list)]])}))

(defn ^:export main [state parent-input global-info responders]
  "Output a table listing all lists in a mine."
  (reagent/create-class
   {:reagent-render
    (fn [{:keys [state upstream-data communication]}]
      [:div
       [:table {:class "list-chooser"}
        [:thead
         [:tr
          [:th "Type"]
          [:th "#"]
          [:th "Name"]]]
        [:tbody
         (for [l @lists]
           ^{:key (.-name l)} [list-row l communication state])]]])
    :component-did-mount
    get-lists}))
