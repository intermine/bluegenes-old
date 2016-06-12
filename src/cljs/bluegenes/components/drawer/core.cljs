(ns bluegenes.components.drawer.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn new []
  (fn []
    [:div.heading
     [:div.new [:div.btn.btn-default
                {:on-click (fn [] (re-frame/dispatch [:new-search]))}
                "New Search"]]]))

(defn no-research []
  (fn []
    [:div.heading
     [:span "No saved data."]]))

(defn operations []
  (fn []
    [:div.heading
     [:div.btn.btn-primary
      {:on-click (fn [] (re-frame/dispatch [:set-active-panel :operations-panel]))} "Operations"]]))



(defn item []
  (let [handle-key (fn [{id :_id} ks]
                     (if (= 13 (-> ks .-which))
                       (re-frame/dispatch [:relabel-research id (-> ks .-target .-value)])))
        active-network (re-frame/subscribe [:active-network])]
    (reagent/create-class
      {:component-did-mount (fn [e]
                              (if-let [tb (sel1 (reagent/dom-node e) :input)]
                                (.focus tb)))
       :reagent-render      (fn [{:keys [payload _id label saved data editing view slug] :as details}]
                              ;(println "saved" saved)
                              ;(println "DETAILS" details)
                              ;(println "active network" @active-network)
                              [:div.item
                               ;{:on-click (fn []
                               ;             (if-not editing
                               ;               (re-frame/dispatch [:load-research _id])))
                               ; :class (if (= _id @active-network) "active")}


                               [:span.fa-2x.ico
                                [:svg.icon.molecule
                                 [:use {:xlinkHref "#molecule"}]]]
                               [:span.grow
                                (if editing
                                  [:input.form-control
                                   {:type         "textarea"
                                    :on-key-press (partial handle-key details)
                                    :rows         5
                                    :placeholder  "Label your research..."}]
                                  [:span
                                   {:on-click #(re-frame/dispatch [:set-saved-data-panel
                                                                   :saved-data-panel "project1" slug])}
                                   [:a label]])]
                               [:span.count
                                [:span.big (str (-> payload :count))]
                                [:span.right (str (-> payload :data :type) "s")]]
                               ])})))

(defn main []
  (let [saved-research (re-frame/subscribe [:saved-research])]
    (fn []
      [:div.drawer
       [:div.heading [:h3 "Saved Data"]]
       (if (or (nil? @saved-research) (empty? @saved-research))
         [no-research]
         (do
           (for [[id details] @saved-research]
            ^{:key id} [item details])))
       [operations]])))