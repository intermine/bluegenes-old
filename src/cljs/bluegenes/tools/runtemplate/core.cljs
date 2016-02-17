(ns bluegenes.tools.runtemplate.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))


(defn template-matches-pathtype? [path template]
  "True if a template has a constraint with a cerain type"
  (if (some (fn [constraint]
              (= (get constraint "path") path))
            (get (second template) "where"))
    template))

(defn filter-templates-for-type [path templates]
  "Filter a collection of templates for a certain path type"
  (filter #(template-matches-pathtype? path %) templates))

(defn valid-templates [type tpls]
  "Get templates that can use our input type"
  (filter-templates-for-type type tpls))



(defn constraint []
  (fn [con update-fn]
    [:div
     [:form
      [:div.form-group
       [:label (get con "path")]
       [:div.input-group
        [:span.input-group-addon (get con "op")]
        [:input.form-control {:type "text"
                              :value (if (nil? (get con "value"))
                                       (get con "values")
                                       (get con "value"))
                              :disabled (if (= true (get con "fixed")) "true")
                              :on-change (fn [e]
                                           (update-fn con {"value" (.. e -target -value)}))}]]]]]))

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
                :value (js->clj (->  @state))}}))

(defn run-button [state emit]
  [:button.btn.btn-info.btn-raised {:on-click (fn [e] (run-button-handler state emit)) } "Run"])

(defn convert-input-to-constraint [input]
  (cond
    (= (get-in input [:data :format]) "list")
    (do
      {"path" (str (get-in input [:data :type]) "")
       "op" "IN"
       "value" (get-in input [:data :name])
       "hide" true
       "fixed" true})

    (= (get-in input [:data :format]) "ids")
    {"path" (str (get-in input [:data :type]) ".id")
     "op" "ONE OF"
     "hide" true
     "values" (get-in input [:data :value])
     "fixed" true}


     ))

(defn replace-input-constraint [input template]
  (update-in template ["where"] #(map (fn [con]
                                        (if (true? (= (get con "path") (get-in input [:data :type])))
                                          (merge (dissoc con "value" "values") (convert-input-to-constraint input))
                                          con)) %)))

; (defn drop-down-handler-old [func input templates e]
;   (let [name (-> e .-target .-value)]
;     (func {:selected name
;      :query (replace-input-constraint (get templates name) input)})))

(defn fetch-templates [local-state]
  "Store Intermine's templates in our local state atom"
  (-> (js/imjs.Service. #js {:root "www.flymine.org/query"})
      (.fetchTemplates)
      (.then (fn [response]
               (swap! local-state assoc :all-templates (js->clj response))))))

(defn updater [comp]
  (let [{:keys [upstream-data]} (reagent/props comp)]
      (println "new upstream data" upstream-data)))

(defn drop-down []
  "Render a drop down that only shows our valid templates"
  (fn [{:keys [templates on-change-handler selected]}]
     [:select.form-control
      {:on-change on-change-handler
       :value selected}
      (doall
        (for [[name values] templates]
          ^{:key name} [:option {:value name} (get values "title")]))]))


(defn store-filtered-templates
  "Filter known templates for a given type (ex. Gene) and associate them
  to an atom."
  [local-state template-type all-templates]
  (swap!
   local-state
   assoc :filtered-templates
   (valid-templates template-type all-templates)))


(defn on-select [templates api e]
  (let [template-name (-> e .-target .-value)]
    (-> {:query (get templates template-name)}
        ((:append-state api)))))


(defn default-button [e]
  (fn []
    [:div.btn.btn-raised "Defaults"]))

(defn save-query [api templates e]
  (-> {:query (get templates (.. e -target -value))}
      ((:append-state api))))

(defn replace-constraints [query cons replace]
   (swap! query update-in ["where"] (fn [constraints]
                                      (doall
                                        (map
                                         (fn [constraint]
                                           (if (= constraint cons)
                                             (merge constraint replace)
                                             constraint))
                                         constraints)))))

(defn ^:export main [props]
  (let [query (reagent.core/atom {})
        local-state (reagent.core/atom {:all-templates nil
                                        :filtered-templates nil})]
    (reagent/create-class
     {:component-did-mount
      (fn [e]
        (fetch-templates local-state))
      :component-will-receive-props
      (fn [this comp]
        (let [old-props (reagent/props this)
              new-props (extract-props comp)]
          (->> (:query (:state new-props))
               (replace-input-constraint (:upstream-data new-props))
               (reset! query))))

      :reagent-render
      (fn [{:keys [api]}]
        [:div
         [drop-down {:templates (:all-templates @local-state)
                     :on-change-handler (partial save-query
                                                 api
                                                 (:all-templates @local-state))}]
         ; Build our constraints DIV
         (into [:div]
               (map (fn [where]
                      [constraint where (partial replace-constraints query)])
                    (get @query "where")))

        ;  [:div.btn.btn-primary.btn-raised
        ;   {:on-click (fn [e] (println query))}
        ;   "run"]
         [run-button query (:has-something api)]
         [default-button]])})))
