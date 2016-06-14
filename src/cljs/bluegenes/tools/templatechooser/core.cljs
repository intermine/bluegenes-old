(ns bluegenes.tools.templatechooser.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [bluegenes.tools.templatechooser.helpers :as h]
            [bluegenes.utils.imcljs :as imcljs]))

(enable-console-print!)

;(defn process-query [query model payload]
;  (println "query" (update-in query [:where]
;                              (fn [constraints]
;                                (h/replace-input-constraint model constraints "Gene" )
;                                ))))


(defn template []
  (fn [{:keys [step-data template]}]
    (fn []
      (let [[id query] template
            save-state (-> step-data :api :save-state)]
        [:a.list-group-item
         {:on-click (fn [] (save-state id))}
         [:h4.list-group-item-heading
          (last (clojure.string/split (:title query) "-->"))]
         [:p.list-group-item-text (:description query)]]))))


(defn templates []
  (fn [step-data]
    ; FAST
    (into [:div.list-group]
          (for [t (-> step-data :cache :runnable)]
            [template {:step-data step-data
                       :template  t}]))

    ; HOLY HELL THIS IS SLOW
    ;[:div.list-group
    ; (for [t (-> step-data :cache :runnable)]
    ;   (do
    ;     (println "test")
    ;     [template {:step-data step-data :template  t}]))]
    ))

(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [snapshot
   {:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache] :as api}
   global-cache]

  ;(println "what changed" what-changed)
  (println "run is running" what-changed)
  (if (or (nil? cache) (not (nil? input)))
    (do
      (println "template running ")
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
        (save-cache {:runnable transformed}))))

  (if (or (contains? what-changed :state) (contains? what-changed :cache))
    (do
      (println "payload" (get-in snapshot [:cache :runnable (:state snapshot)]))
      (has-something {:service  {:root "www.flymine.org/query"}
                     :data     {:format  "query"
                                :type    "Gene"
                                :payload (get-in snapshot [:cache :runnable (:state snapshot)])}
                     :shortcut "viewtable"}))))

(defn upstream-data []
  (fn [data]
    [:div (str data)]))

(defn ^:export main []
  (reagent/create-class
    {:reagent-render (fn [{:keys [state cache api global-cache] :as step-data}]
                       [:div [templates step-data]])}))

