(ns bluegenes.tools.idresolver.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))

(def search-results (reagent.core/atom {:results nil}))

(defn identifier-input [state]
  [:textarea.form-control
    { :value @state
      :on-change (fn [val]
        (reset! state (-> val .-target .-value)))}])

(defn results-handler [values mine comm]
  (let [matches (-> values (aget "matches") (aget "MATCH"))]
    ((:has-something comm) {:data {:format "ids"
                                   :values (into [] (map #(aget % "id") matches))
                                   :type "Gene"}
                            :service {:root "www.flymine.org/query"}})))

(defn submit-handler [values comm]
  ;let's check for a history id. if we have one, it's an existing history, if not, maybe we're on the homepage; we'll need to make a history.
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        id-promise (-> mine (.resolveIds (clj->js
          {:identifiers (map str/trim (str/split values ","))
           :type "Gene"
           :extra "D. melanogaster"})))]

    (-> id-promise (.then (fn [job-id] (.poll job-id (fn [success] (results-handler success mine comm))))))))

(defn submit-button [value api]
  [:button.btn
    {:on-click (fn [e]
      (let [identifiers @value]
        ((:append-state api) {:input identifiers})
        (submit-handler identifiers api)))} "Submit"])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn [{:keys [state upstream-data api]}]
        [:div
          [:div
            [:label "Upload your list of identifiers (Genes, Proteins, etc.)"]
            [identifier-input local-state]]
         [:div.form-group
          [submit-button local-state api]]])
      :component-did-mount (fn [this]
        (identifier-input local-state))
      :component-did-update (fn [this old-props]
        (.log js/console "did update" this old-props))})))
