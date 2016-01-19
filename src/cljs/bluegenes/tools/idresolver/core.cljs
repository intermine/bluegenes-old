(ns bluegenes.tools.idresolver.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [intermine.imjs :as imjs]))

(def search-results (reagent.core/atom {:results nil}))

(defn identifier-input [value]
  [:textarea.form-control {:value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn results-handler [values mine comm]
  (let [matches (-> values (aget "matches") (aget "MATCH"))]
    ((:has-something comm) {:data {:format "ids"
                                   :values (into [] (map #(aget % "id") matches))
                                   :type "Gene"}
                            :service {:root "www.flymine.org/query"}})))

(defn submit-handler [values comm]
  (let [mine (js/imjs.Service. (clj->js {:root "www.flymine.org/query"}))
        id-promise (-> mine (.resolveIds (clj->js {:identifiers (map str/trim (str/split values ","))
                                                   :type "Gene"
                                                   :extra "D. melanogaster"})))]

    (-> id-promise (.then (fn [job-id] (.poll job-id (fn [success] (results-handler success mine comm))))))))

(defn submit [value comm]
  [:button.btn {:on-click (fn [e]
                            (let [identifiers @value]
                              ((:append-state comm) {:input identifiers})
                              (submit-handler identifiers comm)))} "Submit"])

(defn ^:export main []
  (fn [input comm]
    (let [input-atom (reagent.core/atom (-> input :state last :input))]
      (if-not (empty? (-> input :state))
        (submit-handler (-> input :state last :input) comm))
    [:div
     [:div.form-group
      [:label "Identifiers"]
      [identifier-input input-atom]]
     [:div.form-group
      [submit input-atom comm]]])))
