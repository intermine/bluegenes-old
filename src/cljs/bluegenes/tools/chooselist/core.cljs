(ns bluegenes.tools.chooselist.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.components.paginator :as paginator]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]
            [bluegenes.utils.imcljs :as im]))

(enable-console-print!)

; TODO: This should be passed into the tool as a property.
(def flymine {:root "www.flymine.org/query"})
(def pager (reagent/atom
             {:current-page  0
              :rows-per-page 10}))

(defn get-lists-old
  "Fetch lists from flymine and store them to the list atom.
  Transforms the resulting javascript list array to a clojure map
  where the key is a listname.
  {list1 #js{list1-details}
   list2 #js{list2-details}}"
  [local-state]
  (-> (js/imjs.Service. (clj->js flymine)) .fetchLists
      (.then (fn [im-lists]
               (swap! pager assoc :rows (count im-lists))
               (swap! local-state assoc
                      :results (partition-all (:rows-per-page @pager) im-lists))))))

(defn get-lists
  "Fetch lists from flymine and store them to the list atom.
  Transforms the resulting javascript list array to a clojure map
  where the key is a listname.
  {list1 #js{list1-details}
   list2 #js{list2-details}}"
  [local-state]
  (-> (js/imjs.Service. (clj->js flymine)) .fetchLists
      (.then (fn [im-lists]
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
                                (.log js/console "CLICK" list-name)
                                ((:has-something api)
                                  {:data
                                             {:format  "list"
                                              :type    (.-type list-value)
                                              :payload list-name}
                                   :service  flymine
                                   :shortcut "viewtable"}))
                    :class    (if (is-selected list-value state)
                                "selected")}
        [:td [:span {:class (str "type-" (.-type list-value) " result-type")} (.-type list-value)]]
        [:td {:class "count"} (.-size list-value)]
        [:td {:class "list-name"} (.-name list-value)]])}))

(defn did-update-handler
  "When this tool is updated and it has a 'chose' value in its state
  then re-emit the output to the API."
  [local-state {:keys [state api]}]
  (when-let [list-details (get @local-state (:chose state))]
    (-> {:service flymine
         :data    {:format  "list"
                   :type    (.-type list-details)
                   :payload (.-name list-details)}}
        ((:has-something api)))))

(defn pagination-handler [new-page-num]
  (swap! pager assoc :current-page (- new-page-num 1))
  )

(defn pagination-control []
  [paginator/main
   {:current-page  (+ (:current-page @pager) 1)
    :spread        1
    :rows          (:rows @pager)
    :rows-per-page (:rows-per-page @pager)
    :on-change     pagination-handler
    }])

(defn ^:export run [input state cache {:keys [save-state save-cache]}]
  (cond
    (nil? cache) (go (let [lists (<! (im/lists {:service {:root "www.flymine.org/query"}}))]
                       (save-cache {:lists lists}))))
  (save-state {:TESTING 123}))

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (let [local-state (reagent/atom nil)]
    (reagent/create-class
      {:reagent-render
       (fn [{:keys [state cache upstream-data api] :as step-data}]
         (println "GOT STEP DATA" cache)
         [:div
          [pagination-control]
          [:table {:class "list-chooser"}
           [:thead
            [:tr
             [:th "Type"]
             [:th "#"]
             [:th "Name"]]]
           [:tbody
            (for [result (:lists cache)]
              [:tr
               [:td (:type result)]
               [:td (:size result)]
               [:td (:name result)]]
              ;^{:key (.-name result)}
              ;[list-row (.-name result) result api state]
              )]]
          ])
       :component-did-mount
       (fn [this]
         (get-lists local-state))
       :component-did-update
       (fn [this old-props]
         (did-update-handler local-state (reagent/props this)))})))
