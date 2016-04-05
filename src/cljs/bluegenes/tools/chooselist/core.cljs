(ns bluegenes.tools.chooselist.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.components.paginator :as paginator]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]))

(enable-console-print!)

; TODO: This should be passed into the tool as a property.
(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))
(def pager (reagent/atom
            {:current-page 0
             :rows-per-page 10}))

(defn get-lists
  "Fetch lists from flymine and store them to the list atom.
  Transforms the resulting javascript list array to a clojure map
  where the key is a listname.
  {list1 #js{list1-details}
   list2 #js{list2-details}}"
  [local-state]
  (-> flymine .fetchLists
    (.then (fn [im-lists]
      (.log js/console "im-lists" (clj->js im-lists) (count im-lists))
      (swap! pager assoc :rows (count im-lists))
      (swap! local-state assoc
             :results (partition-all (:rows-per-page @pager) im-lists))))))

(defn is-selected [list state]
  "Returns true when a list name matches the most recent state (user chosen) list name"
  (= (.-name list) (:chose state)))

(defn list-row []
  "Generates a single list row with  counts and list type."
  (reagent/create-class
   {:reagent-render
    (fn [list-name list-value api state]
      [:tr.result {:on-click (fn []
                        ((:append-state api) {:chose (.-name list-value)}))
            :class (if (is-selected list-value state)
                     "selected")}
       [:td [:span {:class (str "type-" (.-type list-value) " result-type")} (.-type list-value)]]
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

(defn pagination-handler [new-page-num]
  (swap! pager assoc :current-page (- new-page-num 1))
  )

(defn pagination-control []
  [paginator/main
   {:current-page (+ (:current-page @pager) 1)
    :spread 1
    :rows (:rows @pager)
    :rows-per-page (:rows-per-page @pager)
    :on-change pagination-handler
    }])

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
           (for [result (nth (:results @local-state) (:current-page @pager))]
             ^{:key (.-name result)}
               [list-row (.-name result) result api state]
            )]]
            [pagination-control]
          ])
      :component-did-mount
      (fn [this]
        (get-lists local-state))
      :component-did-update
      (fn [this old-props]
        (did-update-handler local-state (reagent/props this)))})))
