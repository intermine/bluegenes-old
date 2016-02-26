(ns bluegenes.tools.search.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))


(defn results-handler [results mine comm]
  "Emit our results once the promise comes back."
  (.log js/console "%cresults" "background-color:ivory" results)
  (reset! search-results results)
  )

(defn submit-handler [searchterm comm]
  "Resolves IDs via IMJS promise"
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        search {:q searchterm}
        id-promise (-> mine (.search (clj->js search)))]
    (-> id-promise (.then
        (fn [results]
          (results-handler results mine comm))))))

(defn id-form [local-state api]
  "Visual form component which handles submit and change"
  [:div
  [:form {:on-submit (fn [e]
      (.preventDefault js/e)
      (let [searchterm @local-state]
        ((:append-state api) {:input searchterm})
        (submit-handler searchterm api)))}
        [:input {
          :type "text"
          :placeholder "Search for a gene, protein, disease, etc..."
          :value @local-state
          :on-change (fn [val]
              (reset! local-state (-> val .-target .-value)))}]
   [:div.form-group
    [:button "Submit"]]]
   [:div.results
    @search-results]
    ])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [id-form local-state api])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))]
          (reset! local-state (:input passed-in-state))))
      :component-did-update (fn [this old-props]
        (.log js/console "did update" this old-props))})))
