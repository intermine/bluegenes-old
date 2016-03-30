(ns bluegenes.tools.chooselist.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]))

(enable-console-print!)

; TODO: This should be passed into the tool as a property.
(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(defn get-lists
  "Fetch lists from flymine and store them to the list atom.
  Transforms the resulting javascript list array to a clojure map
  where the key is a listname.
  {list1 #js{list1-details}
   list2 #js{list2-details}}"
  [local-state]
  (-> flymine .fetchLists
      (.then (fn [im-lists]
               (reset! local-state (reduce
                              (fn [col next-list]
                                (assoc col  (.-name next-list) next-list))
                              (sorted-map) im-lists))))))

(defn is-selected [list state]
  "Returns true when a list name matches the most recent state (user chosen) list name"
  (= (.-name list) (:chose state)))

(defn list-row []
  "Generates a single list row with  counts and list type."
  (reagent/create-class
   {:reagent-render
    (fn [list-name list-value api state]
      [:tr {:on-click (fn []
                        ((:append-state api) {:chose (.-name list-value)}))
            :class (if (is-selected list-value state)
                     "selected")}
       [:td {:class (str "type-" (.-type list-value) " list-type")} (.-type list-value)]
       [:td {:class "count"} (.-size list-value)]
       [:td {:class "list-name"} (.-name list-value)]])}))

(defn did-update-handler
  "When this tool is updated and it has a 'chose' value in its state
  then re-emit the output to the API."
  [local-state {:keys [state api]}]
  (when-let [list-details (get @local-state (:chose state))]
    (-> {:service {:root "www.flymine.org/query"}
         :data {:format "list"
                :type (.-type list-details)
                :payload (.-name list-details)}}
        ((:has-something api)))))

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (let [local-state (reagent/atom nil)]
    (reagent/create-class
     {:reagent-render
      (fn [{:keys [state upstream-data api]}]
        [:div
         [:table {:class "list-chooser"}
          [:thead
           [:tr
            [:th "Type"]
            [:th "#"]
            [:th "Name"]]]
          [:tbody
           (for [[name value] @local-state]
             ^{:key name} [list-row name value api state])]]])
      :component-did-mount
      (fn [this]
        (get-lists local-state))
      :component-did-update
      (fn [this old-props]
        (did-update-handler local-state (reagent/props this)))})))
