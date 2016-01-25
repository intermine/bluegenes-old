(ns bluegenes.tools.faketool.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))


(defn filter-for-type [[name template] type]
  (let [wheres (get template "where")]
    (if (true? (some #(= (get % "path") type) wheres))
      template)))


(defn fetch-templates-handler [app-state]
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        tpl-promise (-> mine .fetchTemplates)]
    (-> tpl-promise (.then (fn [templates]
                             (swap! app-state assoc :templates (js->clj templates)))))))



(defn drop-down [app-state local-state comms input]
  [:div
  ;  (println @input)
   (println "list" (-> input :input :data :format))
  ;  (println "selected" (:selected @local-state))
   [:select.form-control
    {:on-change (fn [e]
                  (do (swap! app-state assoc
                        :selected (-> e .-target .-value)
                        :query (get (:templates @local-state) (-> e .-target .-value)))
                        ((:replace-state comms) (dissoc @app-state :templates))
                        ))}
    (doall
      (for [[k v] (filter #(filter-for-type % (get-in input [:input :data :type])) (seq (:templates @local-state)))]
        ^{:key (get v "name")} [:option {:value (get v "name")
                                         :default (if (= (get v "name") (:selected @app-state)) "test")}
                                (get v "title")]))]])

(defn prepare-input-constraint [input con]
  (if (= (get con "path") ("Gene"))
    (assoc con "value" "REPLACED")
    con))

(defn lock-contraint? [con]
  (if (= (get con "path") "Gene")
    true
    false))

(defn constraint [con & [locked]]
  (fn []
    [:div
    ; Hide constraints that can't be changed
    ;  {:class (if (true? locked) "hide")}
     [:form
     [:div.form-group
      [:label (get con "path")]
      [:div.input-group
       [:span.input-group-addon (get con "op")]
       [:input.form-control {:type "text"
                             :value (get con "value")
                             :disabled (if (true? locked) "true")}]]]]]))

(defn constraints [cons]
  [:div
   (for [con cons]
     ^{:key (get con "path")} [constraint con (lock-contraint? con)])])

(defn run [app-state]
 (fn []
   [:div
    [:button.btn.btn-success "Run"]]))

(defn ^:export main [input]
  (let [local-state (reagent.core/atom {:templates nil})]
    (reagent/create-class
     {:component-did-mount  (fn []
                              (fetch-templates-handler local-state))
      :reagent-render       (fn [input comms]
                              (let [app-state (reagent.core/atom (last (:state input)))]
                                [:div
                                ;  [add-state-button comms]
                                 [drop-down app-state local-state comms input]
                                 [constraints (get-in @app-state [:query "where"])]
                                 [run app-state]

                                ;  [:div (str (dissoc @app-state :templates))]
                                 ])
                              )})))
