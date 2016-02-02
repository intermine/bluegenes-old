(ns bluegenes.tools.runtemplate.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))


(defn filter-for-type [[name template] type]
  (let [wheres (get template "where")]
    (if (true? (some #(= (get % "path") type) wheres))
      template)))



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

(defn drop-down [{:keys [templates on-change-handler selected]}]
  "Render a drop down that only shows our valid templates"
  ; (println "RUN WITH TEMPLATE" templa_tes)
  (.log js/console "got selected" selected)
  [:div
   [:select.form-control
    {:on-change on-change-handler
     :value selected}
    (doall
      (for [[name values] templates]
        ^{:key name} [:option {:value name} (get values "title")]))]])

(defn constraint [con & [locked]]
  (fn []
    [:div
    ; Hide constraints that can't be changed
    ;  {:class (if (= (get con "edidtable" false)) "hide")}
     [:form
     [:div.form-group
      [:label (get con "path")]
      [:div.input-group
       [:span.input-group-addon (get con "op")]
       [:input.form-control {:type "text"
                             :value (get con "value")
                             :disabled (if (= false (get con "editable")) "true")}]]]]]))

(defn path-end [path]
  (last (clojure.string/split path #"\.")))

(defn constraints [cons]
  "Renders a list of constraints ignoring any constraints on id."
  [:div
   (for [con cons :when (not (get con "hide"))]
     ^{:key (get con "path")} [constraint con])])

(defn run-button-handler [state emit]
  "Emit the data."
  (emit {:service {:root "www.flymine.org/query"}
         :data {:format "query"
                :type "Gene"
                :value (js->clj (-> (:query @state)))}}))

(defn run-button [state emit]
  [:button.btn.btn-info.btn-raised {:on-click (fn [e] (run-button-handler state emit)) } "Run"])

(defn convert-input-to-constraint [input]
  (cond
    (= (get-in input [:data :format]) "list")
    {"path" (str (get-in input [:data :type]) "")
     "op" "IN"
     "value" (get-in input [:data :name])
     "hide" true}

    (= (get-in input [:data :format]) "ids")
    {"path" (str (get-in input [:data :type]) ".id")
     "op" "ONE OF"
     "hide" true
     "value" (get-in input [:data :values])}))

(defn replace-input-constraint [template input]
  (update-in template ["where"] #(map (fn [con]
                                        (if (true? (= (get con "path") (get-in input [:data :type])))
                                          (merge con (convert-input-to-constraint input))
                                          con)) %)))

(defn drop-down-handler [state templates input e]
  (let [name (-> e .-target .-value)]
    (swap! state assoc :selected name :query (replace-input-constraint (get templates name) input))))


(defn fetch-templates-handler [local-state]
  "Store Intermine's templates in our local state atom"
  (-> (js/imjs.Service. #js {:root "www.flymine.org/query"})
      (.fetchTemplates)
      (.then (fn [response]
               (swap! local-state assoc :templates (js->clj response))))))



(defn ^:export main [step-data comms]
  (let [local-state (reagent.core/atom {:templates nil})
        persistent-state (reagent.core/atom (last (:state step-data)))]
    (reagent/create-class
     {:component-did-mount (fn [e]
                             ; Fetch templates when first mounted, once.
                             (fetch-templates-handler local-state))

      :component-did-update (fn []
                              (if-not (nil? (:query @persistent-state))
                                (do
                                  (.log js/console "RERUNNING")
                                  ((:has-something comms) (replace-input-constraint (:query @local-state) (:input step-data)))
                                  ); Run the template again
                                ))

      :reagent-render (fn [step-data {:keys [has-something replace-state]}]
                        [:div
                         [drop-down {:templates (get-valid-templates "Gene" (:templates @local-state))
                                     :selected (:selected @persistent-state)
                                     :on-change-handler (comp replace-state (partial drop-down-handler
                                                                                     persistent-state
                                                                                     (:templates @local-state)
                                                                                     (:input step-data)))}]
                         [constraints (get-in @local-state [:query "where"])]
                         [run-button local-state has-something]
                         ]
                        )})))



; (defn ^:export main [step-data]
;   (let [local-state (reagent.core/atom {:selected nil
;                                         :query nil
;                                         :templates nil})]
;     (reagent/create-class
;      {:component-did-mount  (fn [e]
;                               (fetch-templates-handler local-state))
;       :component-did-update (fn [e]
;                               (if (not (nil? (:query @local-state)))
;                                 (.log js/console "has query, PLEASE RE RUN"))
;                               ; Need to re-run the tool here if we've already
;                               ; been run before.
;                               ; (if (not (nil? (:query @state))))
;                               )
;       :reagent-render (fn [step-data {:keys [has-something
;                                              replace-state]}]
;                         (let [app-state (reagent.core/atom (last (:state step-data)))]
;                           [:div
;                            [drop-down {:templates (get-valid-templates "Gene" (:templates @local-state))
;                                        :on-change-handler (comp replace-state (partial drop-down-handler app-state (:templates @local-state) step-data))}]
;                            [constraints (get-in @app-state [:query "where"])]
;                            [run-button app-state has-something]])
;                         )})))
