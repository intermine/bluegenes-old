(ns bluegenes.tools.dashboard.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn load-tool
  "Invoke a React component by name from the bluegenes namespace and pass
  it some state."
  [name active active-switcher args]
  [:div.step-container
   {
    ;:class (if active "active" "inactive")
    :on-click active-switcher}
   [:div.body
    ;[:h3 "A Tool"]
    ;[:div (str "args: " args)]
    [:div.tool [(-> bluegenes.tools (aget name) (aget "core") (aget "main")) args]]]])

(defn replace-state
  "Replace the global state of the dashboard including all tools."
  [api value]
  ((:replace-state api) value))

(defn has-something
  "Replace the global state of the dashboard including all tools."
  [api value]
  ((:has-something api) value))

(defn replace-substate
  "Replace the state of one tool in the dashboard. Uses the index of the tool
  in the dashboard state's :tools vector."
  [dashboard-state index state]
  (assoc-in dashboard-state [:tools index :state] [state]))

(defn main []
  "Provides a container for housing multiple tools in the form of a dashboard.
  This tool's state is expected to contain a :tools collection. Each item in
  the collection is a map representing each subtool.
  {:tool 'dashboard'
   :state [{:tools [{:tool 'idresolver' :state []}
                    {:tool 'listchooser' :state []}]"
  (fn [{:keys [state upstream-data api] :as dashboard}]
    [:main.lists-page
     [:div.cards
      (map-indexed (fn [index {:keys [tool state]}]
                     (load-tool tool
                                (= index (:active (:state dashboard)))
                                (fn [x] (println "clicked")) ;(partial (:replace-state api) (assoc (:state dashboard) :active index))
                                {:state (last state)
                                 :upstream-data upstream-data
                                 :api {:replace-state (comp (partial replace-state api) (partial replace-substate (:state dashboard) index))
                                       :has-something (:has-something api)}}))
                   (:tools state))]]))