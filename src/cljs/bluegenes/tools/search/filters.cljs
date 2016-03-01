(ns bluegenes.tools.search.filters
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))
(enable-console-print!)

(defn filter-by [criterion state]
  (swap! state assoc :active-filter criterion)
)

(defn is-active [name active]
  (= name active))

(defn facet-display [state]
  (let [facets (:facets @state) active (:active-filter @state)]
  [:div.facets
    [:h4 "Filter by:"]
      [:div "Active filters: " (:active-filter @state)]
      [:div
       [:h5 "Organisms"]
       [:table
        (for [[name value] (:organisms facets)]
          ^{:key name}
          [:tr
           [:td.count value]
           [:td name]])]
       [:h5 "Categories"]
       [:table
      (for [[name value] (:category facets)]
        ^{:key name}
        [:tr {
            :on-click (fn [e] (filter-by name state))
            :class (if (is-active name active) "active")}
         [:td.count.result-type {:class (str "type-" name)} value]
         [:td name]])]
       ]]))
