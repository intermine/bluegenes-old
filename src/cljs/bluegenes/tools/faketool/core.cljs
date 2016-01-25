(ns bluegenes.tools.faketool.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))


(defn filter-for-type [[name template] type]
  (let [wheres (get template "where")]
    (if (true? (some #(= (get % "path") type) wheres))
      template)))

(defn fetch-templates-handler [local-state]
  "Store Intermine's templates in our local state atom"
  (-> (js/imjs.Service. #js {:root "www.flymine.org/query"})
      (.fetchTemplates)
      (.then (fn [response]
               (swap! local-state assoc :templates (js->clj response))))))

(defn template-matches-pathtype? [path template]
  "True if a template has a constraint with a cerain type"
  (if (some (fn [constraint]
              (= (get constraint "path") path))
            (get (second template) "where"))
    template))

(defn get-templates-for-type [path templates]
  "Filter a collection of templates for a certain path type"
  (filter #(template-matches-pathtype? path %) templates))

(defn get-valid-templates [type tpls]
  "Get templates that can use our input type"
  (get-templates-for-type type tpls))

(defn drop-down [{:keys [templates on-change-handler]}]
  "Render a drop down that only shows our valid templates"
  ; (println "RUN WITH TEMPLATE" templates)
  [:div
   [:select.form-control
    {:on-change on-change-handler}
    (doall
      (for [[name values] templates]
        ^{:key name} [:option {:value name} (get values "title")]))]])

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

(defn drop-down-handler [state templates e]
  (let [name (-> e .-target .-value)]
    (swap! state assoc :selected name :query (get templates name))))

(defn ^:export main [input]
  (let [local-state (reagent.core/atom {:templates nil})]
    (reagent/create-class
     {:component-did-mount  (fn []
                              (fetch-templates-handler local-state))
      :reagent-render       (fn [input {:keys [has-something
                                               replace-state]}]
                              (let [app-state (reagent.core/atom (last (:state input)))]
                                [:div
                                 [drop-down {:templates (get-valid-templates "Gene" (:templates @local-state))
                                             :on-change-handler (comp replace-state (partial drop-down-handler app-state (:templates @local-state)))}]
                                 [constraints (get-in @app-state [:query "where"])]]))})))
