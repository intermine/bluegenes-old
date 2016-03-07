(ns bluegenes.components.listdropdown
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [bluegenes.utils.imcljs :as im]))



(defn contains-str? [haystack needle]
  (if (true? (or (nil? needle) (nil? haystack)))
    true
    (let [haystack (.toLowerCase haystack)
          needle (.toLowerCase needle)]
      (if (> (.indexOf haystack needle) -1) true false))))

(defn filter-box []
  (fn [local-state]
    [:input.form-control {:type "text"
                          :value (:filter @local-state)
                          :placeholder "Filter"
                          :on-change (fn [e]
                                       (swap! local-state assoc :filter (.. e -target -value)))}]))


(defn row []
  (fn [data action state]
    [:li [:a {:on-click (fn [e]
                          (swap! state assoc :selected (:title data))
                          (action (:title data)))}
          (str (:title data) " " (:size data) " " (:type data))]]))


(defn main []
  (let [local-state (reagent.core/atom {:filter nil
                                        :lists nil
                                        :selected nil})]


    (reagent/create-class
     {:reagent-render
      (fn [{:keys [title on-change]}]
        [:div.dropdown
         [:div.btn.btn-raised {:data-toggle "dropdown" :class "dropdown-toggle"}
          [:span (if (nil? (:selected @local-state)) "None" (:selected @local-state)) [:span.caret]]]
         [:div.dropdown-menu
          [filter-box local-state]
         (into [:ul]
               (map (fn [e] [row e on-change local-state])
                    (doall
                      (filter #(contains-str? (:title %) (:filter @local-state))
                              (:lists @local-state) ))))]])
      :component-did-mount
      (fn [this]
        (let [{:keys [title service]} (reagent/props this)]
          (go (let [lists (<! (im/lists {:service service}))]
                (swap! local-state assoc :lists lists)))))})))
