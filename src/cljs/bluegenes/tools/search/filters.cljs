(ns bluegenes.tools.search.filters
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]))
(enable-console-print!)

(defn filter-by [criterion state]
  "sets the active filter. Currently this is a single filter type; we may wish to handle multiples in thefuture"
  (swap! state assoc :active-filter criterion)
)

(defn is-active [name active]
  "returns whether a given filter is active"
  (= name active))

(defn display-active-filter [active-filter]
  "Outputs which filter is active (if any)"
  [:div (if (some? active-filter) active-filter "None")]
)

(defn remove-filter [filter-name state]
  (fn [filter-name state]
    [:a
    {:aria-label (str "Remove " filter-name " filter")
     :on-click (fn []
        (swap! state assoc :active-filter "SDFSDFS")
        ;(filter-by nil state)
        ;(println state)
        (println @state)
        (.log js/console "Boogie")
      )}
    "X"]))

(defn facet-display [state]
  (let [facets (:facets @state) active (:active-filter @state)]
  [:div.facets
   [:div (json-html/edn->hiccup @state)]
    [:h4 "Filter by:"]
      [:div "Active filters: "
       [display-active-filter active]]
      [:div
       ;;TODO: Re-implement this filter when we implement RESTful server-side filters
      ;  [:h5 "Organisms"]
      ;  [:table
      ;   (for [[name value] (:organisms facets)]
      ;     ^{:key name}
      ;     [:tr
      ;      [:td.count value]
      ;      [:td name]])]
       [:h5 "Categories"]
       [:table
      (for [[name value] (:category facets)]
        ^{:key name}
        [:tr {
            :on-click (fn [e] (filter-by name state))
            :class (if (is-active name active) "active")}
         [:td.count.result-type {:class (str "type-" name)} value]
         [:td name (if (is-active name active)
           [remove-filter name state])]])]
       ]]))
