(ns bluegenes.components.listdropdown
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.utils.imcljs :as im]))



(defn contains-str?
  "Return true if a string contains a substring, or if either
  the string or substring are nil. Matches are not case sensitive."
  [haystack needle]
  (if (true? (or (nil? needle) (nil? haystack)))
    true
    (let [haystack (.toLowerCase haystack)
          needle (.toLowerCase needle)]
      (if (> (.indexOf haystack needle) -1) true false))))

(defn filter-box
  "Text box to filter templates by name."
  []
  (fn [local-state]
    [:input.form-control {:type "text"
                          :value (:filter @local-state)
                          :placeholder "Filter"
                          :on-change (fn [e]
                                       (swap! local-state assoc :filter (.. e -target -value)))}]))

(defn row
  "Represents a selectable list in the drop down."
  []
  (fn [data action state]
    [:li [:a {:on-click (fn [e]
                          (swap! state assoc :selected (:title data))
                          (action (:title data)))}
          (str (:title data) " " (:size data) " " (:type data))]]))


(defn main
  "Provides a dropdown for selecting a list from a service.
  Expects a map of options:
  {:on-change A fn to give the list name when the value changes}"
  []
  (let [local-state (reagent.core/atom {:filter nil
                                        :lists nil
                                        :selected nil})]
    (reagent/create-class
     {:reagent-render
      (fn [{:keys [on-change]}]
        [:div.dropdown
         [:div.btn.btn-raised {:data-toggle "dropdown"
                               :class "dropdown-toggle"}
          [:span (if (nil? (:selected @local-state))
                   "None"
                   (:selected @local-state)) [:span.caret]]]
         [:div.dropdown-menu
          [filter-box local-state]
          (into [:ul]
                (map (fn [e] [row e on-change local-state])
                     (doall
                       (filter (fn [next-list]
                                 (contains-str?
                                  (:title next-list) (:filter @local-state)))
                               (:lists @local-state) ))))]])
      :component-did-mount
      (fn [this]
        (let [{:keys [service]} (reagent/props this)]
          (go (let [lists (<! (im/lists {:service service}))]
                (swap! local-state assoc :lists lists)))))})))
