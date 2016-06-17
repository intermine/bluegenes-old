(ns bluegenes.tools.templatechooser.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.tools.templatechooser.helpers :as h]))

(enable-console-print!)

(defn template []
  (fn []
    (fn [{:keys [step-data template]}]
      (let [[id query] template
            save-state (-> step-data :api :save-state)]
        [:a.list-group-item
         {:class (if (= (:state step-data) id) "active")
          :on-click (fn [] (save-state id))}
         [:h4.list-group-item-heading
          (last (clojure.string/split (:title query) "-->"))]
         [:p.list-group-item-text (:description query)]]))))


(defn templates []
  (fn [step-data]
    (into [:div.list-group]
          (for [t (-> step-data :cache :runnable)]
            [template {:step-data step-data
                       :template  t}]))))

(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache] :as api}
   global-cache]

  (if (or (nil? cache) (not (nil? input)))
    (let [runnable    (into {} (h/runnable (-> global-cache :models :flymine)
                                           (-> global-cache :templates :flymine) "Gene"))
          transformed (into {}
                            (do
                              (cond
                                (= "query" (-> snapshot :input :data :format))
                                (map (fn [[id query]]
                                       [id (h/replace-input-constraints-whole
                                             (-> global-cache :models :flymine)
                                             query
                                             "Gene"
                                             (-> snapshot :input :data :payload :where first))])
                                     runnable)
                                :else
                                (map (fn [[id query]]
                                       [id (h/replace-input-constraints
                                             (-> global-cache :models :flymine)
                                             query
                                             "Gene"
                                             (-> snapshot :input :data :payload))])
                                     runnable))))]
      (save-cache {:runnable transformed})))

  (if (or (contains? what-changed :state) (contains? what-changed :cache))
    (has-something {:service  {:root "www.flymine.org/query"}
                    :data     {:format  "query"
                               :type    "Gene"
                               :payload (get-in snapshot [:cache :runnable (:state snapshot)])}
                    :shortcut "viewtable"})))

(defn upstream-data []
  (fn [data]
    [:div (str data)]))

(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [step-data]
                       [:div [templates step-data]])}))

