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
         {:class    (if (= (:state step-data) id) "active")
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

(defn categories-from-template [[name template-details]]
  (->> (:tags template-details)
       (filter (fn [tag] (re-find #"im:aspect:" tag)))
       (map (fn [tag] (last (clojure.string/split tag #"im:aspect:"))))))

(defn parse-categories [templates]
  (println "PARSING CATEGORIES")
  (into [] (distinct) (mapcat categories-from-template templates)))



(def missing? (complement contains?))

(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [{:keys [cache] :as snapshot}
   {:keys [input-changes state-changes cache-changes] :as what-changed}
   {:keys [has-something save-cache update-cache] :as api}
   global-cache]

  (when (missing? cache :banana)
    (update-cache (fn [c] (assoc c :banana "cakes"))))

  (when (missing? cache :categories)
    (update-cache (fn [c] (assoc c :categories (parse-categories (-> global-cache :templates :flymine))))))

  (when (or input-changes (missing? cache :runnable))
    (update-cache (fn [c] (assoc c :runnable (let [runnable    (into {} (h/runnable (-> global-cache :models :flymine)
                                                                                    (-> global-cache :templates :flymine) "Gene"))
                                                   transformed (into {}
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
                                                                            runnable)))]
                                               (save-cache {:runnable transformed}))))))


  (if (or (contains? what-changed :state) (contains? what-changed :cache))
    (has-something {:service  {:root "www.flymine.org/query"}
                    :data     {:format  "query"
                               :type    "Gene"
                               :payload (get-in snapshot [:cache :runnable (:state snapshot)])}
                    :shortcut "viewtable"})))

(defn menu []
  (fn [step-data]
    [:div "MENU"
     (into [:ul.nav.nav-pills] (map (fn [category] ^{:key (str category)} [:li.active (str category)]) (:categories (:cache step-data))))]))

(defn upstream-data []
  (fn [data]
    [:div (str data)]))

(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [step-data]
                       [:div
                        [menu step-data]
                        [templates step-data]])}))

