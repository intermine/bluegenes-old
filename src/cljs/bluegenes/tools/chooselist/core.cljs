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
  (= (:name list) (:payload (:data state))))

(defn list-row []
  "Generates a single list row with  counts and list type."
  (reagent/create-class
    {:reagent-render
     (fn [list-name list-value api state]
       ;(println "list row called with list value" list-value)
       [:tr.result {:on-click (fn []
                                (.log js/console "CLICK" list-value)
                                ((:save-state api)
                                  {:service {:root "www.flymine.org/query"}
                                   :data    {:payload (:name list-value)
                                             :format  "list"
                                             :type    (:type list-value)}}))
                    :class    (if (is-selected list-value state) "selected")}
        [:td [:span {:class (str "type-" (:type list-value) " result-type")} (:type list-value)]]
        [:td {:class "count"} (:size list-value)]
        [:td {:class "list-name"} (str (:name list-value))]])}))

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

(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache] :as api}]
  (println "Choose List Run Called" what-changed)

  (if (nil? (:cache snapshot))
    (go (let [lists (<! (im/lists {:service {:root "www.flymine.org/query"}}))]
          (save-cache {:lists lists}))))

  (if (contains? state :data)
    (has-something state)))

(defn ^:export main []
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (let [local-state (reagent/atom nil)]
    (reagent/create-class
      {:reagent-render
       (fn [{:keys [state cache api] :as step-data}]
         ;(println "GOT STEP DATA" api)
         [:div
          [pagination-control]
          [:table {:class "list-chooser"}
           [:thead
            [:tr
             [:th "Type"]
             [:th "#"]
             [:th "Name"]]]
           (into [:tbody] (for [result (:lists cache)]
                            ;[:tr
                            ; [:td (:type result)]
                            ; [:td (:size result)]
                            ; [:td (:name result)]]
                            [list-row nil result api state]
                            ;^{:key (.-name result)}

                            ))]
          ])
       :component-did-mount
       (fn [this]
         (get-lists local-state))
       :component-did-update
       (fn [this old-props]
         (did-update-handler local-state (reagent/props this)))})))
