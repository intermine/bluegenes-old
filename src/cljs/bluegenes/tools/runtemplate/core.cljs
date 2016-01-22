(ns bluegenes.tools.runtemplate.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [intermine.imjs :as imjs]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def state2 (reagent/atom {}))



(defn get-id-query [service list]
  {:from "Gene"
   :select "*"
   :where [{:path "Gene.id"
            :values (:values list)
            :op "ONE OF"
            :code "A"
            }]})

; (defn constraint [cons]
;   [:div.form-group
;    [:label (:path cons)]
;    [:input.form-control {:type "text"
;                          :value (:value cons)}]])



; (defn button-runner []
;   [:button.btn.disabled {:disabled "disabled"} "Run"])

; (defn selected-template [has]
;   (println "HAS" has)
;   (fn []
;     (let [templates (:templates @state2)
;           selected (:selected @state2)
;           thekey (keyword (str selected))
;           template (thekey templates)]
;       [:div
;        [:span (str template)]
;        [:h2 (:name template)]
;        [:h4 (:description template)]
;        [:div (doall (for [cons (map #(replace-constraints % (-> has :input :data :values) ) (:where template))] [constraint cons]))]
;        [:span (str (:where template))]])))

(defn template-dropdown2 []
  ; Holds a drop down of lists in an item and populates the selected atom with
  [:select.form-control {:on-change (fn [e]
                                      (swap! state2 assoc :selected (-> e .-target .-value)))}
   (for [item (:filtered @state2)]
     ^{:key (:name item)} [:option
                           {:value (:id item)}
                           (:name item)])])

; (defn ^:export main [data]
;   (fetch-templates)
;   (fn [data]
;     (println "Template tool is rendering.")
;     [:div
;      [template-dropdown]
;      [selected-template data]
;      [button-runner]
;      ]))


(defn replace-input-constraints [cons ids]
  ; Replace the "input" constraint to be that of the previous tool
  (if (= (:path cons) "Gene")
    (assoc cons :values ids :path "Gene.id" :op "ONE OF")
    cons))



(defn filter-for-type [template]
  (let [wheres (get-in template [:where])]
    (if (true? (some #(= (:path %) "Gene") wheres))
      template)))

(defn fetch-templates [s]
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        templates (-> mine .fetchTemplates)]
    (-> templates (.then (fn [r]
                           (let [results (js->clj r :keywordize-keys true)
                                 chopped (into [] (for [k (keys results)]
                                                    (assoc (get results k) :id k)))]
                             (swap! s assoc :templates results)
                             (swap! s assoc :filtered (doall (filter filter-for-type chopped)))
                             ))))))



(defn template-dropdown [state ids]
  ; Holds a drop down of lists in an item and populates the selected atom with
  [:select.form-control {:on-change (fn [e]
                                      (let [name (str (-> e .-target .-value))
                                            tpl ((keyword name) (:templates @state))
                                            updated (update-in tpl [:where] (fn [cons]
                                                                              (map (fn [c] (replace-input-constraints c ids)) cons)))]

                                        (swap! state assoc :query updated)))}
   (for [item (:filtered @state)]
     ^{:key (:name item)} [:option
                           {:value (:id item)}
                           (:name item)])])



; TODO We're currently updating constraints matched on the path and code.
; Is there a better way to do this? What makes a constraint unique?
(defn update-constraint! [e cons state]
  (let [value (str (-> e .-target .-value))
        matcher (fn [c] (and
                         (= (:path cons) (:path c))
                         (= (:code cons) (:code c))))]

    (swap! state update-in [:query :where] (fn [cs] (map #(if (matcher %)
                                                            (assoc % :value value)
                                                            %) cs)))))

(defn constraint [cons state]
  (fn [cons state]
    [:div
     [:div (str cons)]
     [:div.form-group
        [:label (str (:path cons)) " " [:span.badge (:op cons)]]
        [:input.form-control {:type "text"
                              :value (:value cons)
                              :on-change (fn [e] (update-constraint! e cons state))}]]]))


; (defn results-handler [values mine comm]
;   (let [matches (-> values (aget "matches") (aget "MATCH"))]
;     ((:has-something comm) {:data {:format "ids"
;                                    :values (into [] (map #(aget % "id") matches))
;                                    :type "Gene"}
;                             :service {:root "www.flymine.org/query"}})))

(defn submit-button [state emit]
  [:div.form-group
   [:button.btn {:on-click (fn [e]
                             (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
                                   query (clj->js (:query @state))
                                   imquery (.query mine query)]
                               (-> imquery (.then (fn [r]
                                                    (emit {:service {:root "www.flymine.org/query"}
                                                           :data {:format "query"
                                                                  :type "Gene"
                                                                  :value (js->clj (-> r .toJSON))}})
                                                    ; (.log js/console "R" r)
                                                    )))))}
    "Submit"]])

(defn template-details [s]
  [:div
  (for [cons (:where (:query @s))]
    ^{:key (:path cons)} [constraint cons s])])

(defn query-state [q]
  [:div (str (:query @q))])

(defn ^:export main [step-data responders]
  (reagent/create-class
   {:reagent-render (fn []
                      (println ":REAGENT-RENDER")
                      (let [state (reagent/atom {:query nil
                              :selected nil
                              :filtered nil})]
     (fetch-templates state)
     [:div
      [template-dropdown state (-> step-data :input :data :values)]
      [template-details state]
      [submit-button state (get responders :has-something)]
      [query-state state]]))

    ; Runs once immediately after the initial rendering occurs.
    :component-did-mount (fn [e]
                           (.log js/console ":COMPONENT-DID-MOUNT" (clj->js e)))})
  )
