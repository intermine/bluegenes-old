(ns bluegenes.tools.idresolver.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))
(enable-console-print!)

(def search-results (reagent.core/atom {:results nil}))

(defn identifier-input [state]
  (fn []
  [:textarea.form-control
    { :value @state
      :on-change (fn [val]
        (reset! state (-> val .-target .-value)))}]))

(defn results-handler [values mine comm]
  "Emit our results once the promise comes back."
  (let [matches (-> values (aget "matches") (aget "MATCH"))]
    ((:has-something comm) {:data {:format "ids"
                                   :payload (into [] (map #(aget % "id") matches))
                                   :type "Gene"}
                            :service {:root "beta.humanmine.org/beta"}})))

(defn split-ids [values]
  "separate the results from the text area into discrete identifiers"
  (map str/trim (str/split values ",")))



(defn submit-handler [values comm]
  "Resolves IDs via IMJS promise"
  (let [mine (js/imjs.Service. (clj->js {:root "beta.humanmine.org/beta"}))
        query {:identifiers (split-ids values)
         :type "Gene"
         :extra "D. melanogaster"}
        id-promise (-> mine (.resolveIds (clj->js query)))]
    (-> id-promise (.then
        (fn [job-id]
          (.poll job-id (fn [success] (results-handler success mine comm))))))))

(defn id-form [local-state api]
  "Visual form component with onsubmit behaviour"
  [:form  {:on-submit (fn [e]
      (.preventDefault js/e)
      (let [identifiers @local-state]
        ((:append-state api) {:input identifiers})
        (submit-handler identifiers api)))}
    [:div
      [:label "Upload your list of identifiers (Genes, Proteins, etc.)"]
      [identifier-input local-state]]
   [:div.form-group
    [:button "Submit"]]])

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
