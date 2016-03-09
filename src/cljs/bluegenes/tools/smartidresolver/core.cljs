(ns bluegenes.tools.smartidresolver.core
    (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [bluegenes.tools.enrichment.controller :as c]
            [bluegenes.utils.imcljs :as im]
            [json-html.core :as json-html]
            [reagent.impl.util :as impl :refer [extract-props]]))


(def test-identifiers [{:identifier "ABC" :status "matched"}
                       {:identifier "XYZ" :status "matched"}])

(defn strip-characters
  "Removes one or more characters from a string"
  [haystack needles]
  (clojure.string/trim (apply str (remove #((set needles) %) haystack))))

(defn call
  "Calls a function with a set of arguments.
  Arguably easier to read when calling functions from
  a map of functions. Example:
  (call (:has-something api) param1 param2)"
  [f & args]
  (apply f args))

(defn get-status-from-bank
  "Lazily checks the bank of results and returns the status
  keyword of the first record where the input is present"
  [input bank]
  (:status (second
            (first
             (filter (fn [[id values]]
                       (not (nil? (some #{input} (:input values)))))
                     bank)))))

(defn identifier [input bank]
  ; (println "STATUS" (get-status-from-bank input bank))
  [:div.identifier {:key input
                    :class (get-status-from-bank input bank)} input])


(defn route-response [response persistent-state]

  (println "route response is working with" response)

  (swap! persistent-state
         update-in [:bank]
         merge
         (reduce (fn [total next]
                  ;  (println "can see id" (:id next))
                   (assoc total (keyword (str (:id next))) (assoc next :status :match)))
                 {} (:MATCH (:matches response))))

  (swap! persistent-state
         update-in [:bank]
         merge
         (reduce (fn [total next]
                   ;  (println "can see id" (:id next))
                   (assoc total (keyword (str (:input next))) (assoc next :status :duplicate)))
                 {} (:DUPLICATE (:matches response))))

  (swap! persistent-state
         update-in [:bank]
         merge
         (reduce (fn [total next]
                   (assoc total (keyword (str next)) {:status :unresolved
                                                      :input [next]}))
                 {} (:unresolved response))))

(defn resolve-id [identifier persistent-state]
  (go (let [res (<! (im/resolve-ids
                     {:service {:root "beta.flymine.org/beta"}}
                     {:identifiers (if (string? identifier) [identifier] identifier)
                      :type "Gene"
                      :caseSensitive false
                      :wildCards true
                      :extra "D. melanogaster"}))]
        (route-response (-> res :body :results) persistent-state))))

(defn run-job [state]
  (doall (for [[k v] (filter (fn [[k v]] (= (:status v) :new)) (:bank @state))]
           (do
             (swap! state assoc-in [:bank k :status] :pending)
             (go (let [response (<! (resolve-id (name k) state))]
                   nil))))))


(defn input-box []
  (let [textbox-value (reagent/atom "")
        reset #(set! (-> % .-target .-value) nil)]
    (fn [persistent-state]
      [:input.freeform {:value @textbox-value
                        :type "text"
                        :on-change (fn [e]
                                     (let [split-vals (clojure.string/split (.. e -target -value) #" ")]
                                       (if (> (count split-vals) 1)
                                         (do
                                           (swap! persistent-state
                                                  update-in [:identifiers]
                                                  into
                                                  split-vals)

                                           (swap! persistent-state
                                                  update-in [:bank]
                                                  merge
                                                  (reduce (fn [total next]
                                                            (assoc total
                                                                   (keyword next)
                                                                   {:status :new}))
                                                          {}
                                                          split-vals)
                                                  {:status :new})

                                           (run-job persistent-state))

                                         (reset! textbox-value (clojure.string/trim (.. e -target -value))))))


                        :on-key-down (fn [k]
                                       (let [code (.-which k)]
                                         (if-not (nil? (some #{code} '(9 13 32 188)))
                                           (do
                                             (let [cleansed-val (strip-characters @textbox-value ",; ")]
                                               ; Keep an ordered list of inputs
                                               (swap! persistent-state
                                                      update-in [:identifiers]
                                                      conj cleansed-val)
                                               ; Keep a bank of the resolution results
                                               (swap! persistent-state
                                                      update-in [:bank]
                                                      assoc (keyword cleansed-val)
                                                      {:status :new})
                                               ; Create a new ID resolution job for all new identifiers
                                               (run-job persistent-state)
                                               ; Reset our textbox value
                                               (reset k))))))}])))

(defn smartbox [step-data]
  (let [persistent-state (reagent/atom
                          (merge {:identifiers []
                                  :bank {}
                                  :type "Gene"
                                  :caseSensitive false
                                  :wildCards true
                                  :extra "D. melanogaster"}
                                 (:state step-data)))
        local-state (reagent/atom {:current-page 1
                                   :rows-per-page 20})]
    (fn []
      [:div.smartbox
       (doall (map (fn [next]
              ^{:key next} [identifier next (:bank @persistent-state)])
            (:identifiers @persistent-state)))
       [input-box persistent-state]
      ;  (json-html/edn->hiccup @persistent-state)
       ])))

(defn ^:export main [step-data]
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (let [local-state (reagent/atom {:current-page 1
                                   :rows-per-page 20})]

    (reagent/create-class
     {:reagent-render
      (fn [step-data]
        [:div
         [:h1 "ID Resolution"]
         [smartbox]])
      :component-will-receive-props
      (fn [this new-props]
        (let [{:keys [upstream-data api state]} (extract-props new-props)]))})))
