(ns bluegenes.tools.runtemplate.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [clojure.string :as str]
            ; [ajax.core :refer [GET POST]]
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

(defn filter-input-constraints [templates type]
  "Get templates that can use our input type"
  (filter-templates-for-type type templates))



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
     "fixed" true}))

(defn replace-input-constraint [input template]
  (update-in template ["where"] #(map (fn [con]
                                        (if (true? (= (get con "path") (get-in input [:data :type])))
                                          (merge (dissoc con "value" "values") (convert-input-to-constraint input))
                                          con)) %)))

(defn fetch-templates-chan []
  "Fetch templates from Intermine and return them over a channel"
  (let [templates-chan (chan)]
    (-> (js/imjs.Service. #js {:root "www.flymine.org/query"})
        (.fetchTemplates)
        (.then (fn [response]
                 (go (>! templates-chan (js->clj response))))))
    templates-chan))


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
   (filter-input-constraints all-templates template-type)))


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

(defn template-has-tag? [[name details] tag]
  (some (fn [t] (= t (str "im:aspect:" tag))) (get details "tags")))

(defn get-row-count
  "Reset an atom with the row count of an imjs query."
  [query]
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js #js{:root "www.flymine.org/query"}))
        (.query (clj->js query))
        (.then (fn [q] (.count q)))
        (.then (fn [ct]
                 (go (>! c ct)))))
    c))


(defn filter-single-constraints
  "Returns templates that only have a single constraint."
  [templates]
  (filter (fn [[name details]]
            (< (count (get details "where") 2))) templates))



(defn ^:export preview []
  (let [local-state (reagent/atom {:template-counts {}
                                   :category nil
                                   :all-templates nil
                                   :single-constraint-templates {}
                                   :filtered-templates nil})]
    (reagent/create-class
     {:component-will-update (fn [this new-props]
                               (swap! local-state assoc
                                      :category (:category (extract-props new-props))))

      :component-did-mount (fn [this]
                             (go
                              (let [templates (<! (fetch-templates-chan))
                                    filtered-templates (filter-input-constraints templates "Gene")
                                    ip (reagent/props this)
                                    single-constraint-templates (into {} (filter-single-constraints filtered-templates))
                                    adjusted-input-templates (reduce (fn [m [t-name t]]
                                                      ;  (println "SEES T" t)
                                                       (assoc m t-name (replace-input-constraint ip t)))
                                                     {}
                                                     single-constraint-templates)
                                    ; adjusted (into {} (map (fn [[n t]] (replace-input-constraint ip t)) single-constraint-templates))
                                    ]

                                (println "IP" adjusted-input-templates)
                                ; (println "adjusted" single-constraint-templates)
                                (println "done")
                                ; Update our state our initial, filtered template data
                                (swap! local-state merge
                                       {:all-templates templates
                                        :single-constraint-templates single-constraint-templates
                                        :adjusted-input-templates adjusted-input-templates
                                        :filtered-templates filtered-templates})

                                (doall
                                  (for [[name template] adjusted-input-templates]
                                    (go
                                     (let [count (<! (get-row-count template))]
                                       (swap! local-state assoc-in
                                              [:adjusted-input-templates name :count] count))))))))

      :reagent-render (fn []
                        [:div
                         [:div.heading "Popular Queries"]
                         (doall (for [[name data] (take 5 (filter
                                                    (fn [t] (template-has-tag? t (:category @local-state)))
                                                    (:adjusted-input-templates @local-state)))]
                           (let [t (get-in @local-state [:all-templates name "title"])
                                 adjusted-title (clojure.string/join " " (rest (clojure.string/split t #"-->")))]
                             ^{:key name} [:div.indented (str
                                                 adjusted-title
                                                 " (" (:count data) " rows)")])))
                         [:div.indented.highlighted "More..."]])})))





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
     {:should-component-update
      (fn [this o n]
        (println "run template param diff"
                 (nth (clojure.data/diff (extract-props o)
                                         (extract-props n)) 1)))
      :component-did-mount
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
