(ns bluegenes.tools.search.filters
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))
(enable-console-print!)

(defn facet-display [facets]
  [:div.facets
    [:h4 "Filter by:"]
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
        [:tr
         [:td.count.result-type {:class (str "type-" name)} value]
         [:td name]])]
       ]])
