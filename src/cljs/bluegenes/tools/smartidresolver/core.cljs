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

(defn swap-identifier
  "Swap an identifier in some state for a new value.
  Typical use case: swapping an identifier with duplicates with
  a user selected one."
  [state old-identifier new-value]
  (swap! state
         update-in [:identifiers]
         (fn [identifiers]
           (into []
                 (map (fn [identifier]
                        ; Find our match(es)
                        (if (= (:identifier identifier) old-identifier)
                          ; Construct a new shape for the replaced value
                          {:identifier (:symbol (:summary new-value))
                           :status :match
                           :product (assoc new-value
                                           :input [(:symbol (:summary new-value))])}

                          ; Or return the non-match
                          identifier)) identifiers)))))



(defn duplicate-dropdown
  "A dropdown that houses duplicate matches."
  [input state]
  (fn []
    [:div.dropdown
     [:div.dropdown-toggle {:data-toggle "dropdown"}
      (:identifier input)]
     [:ul.dropdown-menu
      (doall (for [dup (:matches (:duplicates input))]
               [:li [:a
                     {:on-click #(swap-identifier state (:identifier input) dup)}
                     [:div (:symbol (:summary dup))]
                     [:div (:primaryIdentifier (:summary dup))]]]))]]))

(defn identifier
  "An element representing user input conditional upon its status."
  [input state]
  (cond
    ; Matches (Good)
    (= (:status input) :match)
    [:div.identifier {:class (:status input)}
     (:identifier input)]
    ; Pending (In progress...)
    (= (:status input) :pending)
    [:div.identifier {:class (:status input)}
     (:identifier input)
     [:i.fa.fa-circle-o-notch.fa-spin]]
    ; Duplicate (ambigious)
    (= (:status input) :duplicate)
    [:div.identifier {:class (:status input)}
     [duplicate-dropdown input state]
     [:i.fa.fa-exclamation-triangle]]
    ; Converted (hopefully good?)
    ; TODO: is there a case when a converted type
    ; has more than one value? If so, dropdown please.
    (= (:status input) :converted)
    [:div.identifier {:class (:status input)}
     (:identifier input)
     [:i.fa.fa-random]]
    ; Catch the rest.
    :else
    [:div.identifier {:class (:status input)}
     (:identifier input)
    ;  [:i.fa.fa-times]
     ]))

(defn resolve-id
  "Resolves an ID from Intermine."
  [identifier settings]
  (go (let [res (<! (im/resolve-ids
                     {:service {:root "beta.flymine.org/beta"}}
                     {:identifiers (if (string? identifier) [identifier] identifier)
                      :type "Gene"
                      :caseSensitive false
                      :wildCards true
                      :extra (:extra settings)}))]
        (-> res :body :results))))


(defn parse-response
  "Parse the ID resolution response and update the identifier in the state
  with the results. So stateful..."
  [state response]
  (let [matches (:MATCH (:matches response))
        unresolveds (:unresolved response)
        duplicates (:DUPLICATE (:matches response))
        converteds (:TYPE_CONVERTED (:matches response))]

    ; Handle unresolved
    (swap! state
           update-in [:identifiers]
           (fn [identifiers]
             (into [] (map (fn [identifier]
                             (if (some #{(:identifier identifier)} unresolveds)
                               (assoc identifier :status :unresolved)
                               identifier))
                           identifiers))))
    ; Handle duplicates
    (swap! state
           update-in [:identifiers]
           (fn [identifiers]
             (into [] (map (fn [identifier]
                             (let [found-dups (first (filter (fn [dup] (= (:identifier identifier) (:input dup))) duplicates))]
                               (if-not (empty? found-dups)
                                 (assoc identifier
                                        :status :duplicate
                                        :duplicates found-dups)
                                 identifier)))
                           identifiers))))
    ; Handle converted types
    (swap! state
           update-in [:identifiers]
           (fn [identifiers]
             (into [] (map (fn [identifier]
                             (let [found-convert (first (filter (fn [dup] (= (:identifier identifier) (:input dup))) converteds))]
                               (if-not (empty? found-convert)
                                 (assoc identifier
                                        :status :converted
                                        :product (if (= 1 (count (:matches found-convert)))
                                                   (first (:matches found-convert))
                                                   found-convert
                                                   ))
                                 identifier)))
                           identifiers))))

    ; Iterate over matches (which can have multiple inputs)
    (for [match matches]
      (let [inputs (:input match)]
        (swap! state
               update-in [:identifiers]
               (fn [identifiers]
                 (into [] (map (fn [identifier]
                                 (if (some #{(:identifier identifier)} (:input match))
                                   (assoc identifier
                                          :status :match
                                          :product match)
                                   identifier))
                               identifiers))))))))

(defn run-job
  "Resolve all identifiers with an input status of :new"
  [state]
  (doall
    (for [identifier (filter (fn [i] (= :new (:status i))) (:identifiers @state))]
      (do
        ; Update the identifier's status to :pending
        (swap! state
               assoc :identifiers
               (into [] (map (fn [i]
                               (if (= (:identifier identifier) (:identifier i))
                                 (assoc i :status :pending)
                                 i))
                             (:identifiers @state))))
        ; Resolve the ID
        (go (let [response (<! (resolve-id (:identifier identifier) @state))]
              (doall (parse-response state response))))))))




(defn input-box
  "Component to handle user input.
  TODO: currently a flexy container but it doesn't wrap when the length
  of the input exceeds the remaining space."
  []
  (let [textbox-value (reagent/atom "")
        reset #(set! (-> % .-target .-value) nil)]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (let [node (reagent/dom-node this)]
          (.focus node)))
      :reagent-render
      (fn [persistent-state]
        [:input.freeform {:value @textbox-value
                          :type "text"
                          :on-change (fn [e]
                                       (reset! textbox-value (clojure.string/trim (.. e -target -value))))
                          :on-key-down (fn [k]
                                         (let [code (.-which k)]
                                           ; Check for breaking characters...
                                           (if-not (nil? (some #{code} '(9 13 32 188)))
                                             ; Break! Parse that puppy.
                                             (do
                                               (let [cleansed-val (strip-characters @textbox-value ",; ")]
                                                 ; Keep an ordered list of inputs
                                                 (swap! persistent-state
                                                        update-in [:identifiers]
                                                        conj {:identifier cleansed-val
                                                              :status :new})
                                                 ; Create a new ID resolution job for all new identifiers
                                                 (run-job persistent-state)
                                                 ; Reset our textbox value
                                                 (reset k))))))}])})))

(defn handle-values
  "Proceed with the bluegenes workflow.
  TODO: call an API function like :has-something"
  [values api]
  ((:has-something api) {:data
                         {:format "ids"
                          :type "Gene"
                          :payload (remove nil? (map #(-> % :product :id) (:identifiers values)))}
                         :service {:root "beta.flymine.org/beta"}
                         :shortcut "viewtable"
                         })
  (println (doall (map #(-> % :product :id) (:identifiers values)))))

(defn stats
  "A container representing statistics about the resolution job."
  []
  (fn [identifiers]
    [:div.stats
     [:div.stat (str (count (filter #(= (:status %) :match) identifiers)) " Matches")]
     [:div.stat (str (count (filter #(= (:status %) :duplicate) identifiers)) " Duplicates")]
     [:div.stat (str (count (filter #(= (:status %) :converted) identifiers)) " Converted")]
     [:div.stat (str (count (filter #(= (:status %) :unresolved) identifiers)) " Unresolved")]]))


(defn dropdown
  []
  (fn [{:keys [values handler value]}]
    [:div.dropdown
     [:div.btn.btn-default.dropdown-toggle {:data-toggle "dropdown"}
      (str value)
      [:span.caret]]
     [:ul.dropdown-menu
      (doall (for [value values]
               ^{:key value} [:li [:a
                     {:on-click (fn [e]
                                  (println "calling on value" value)
                                  (handler value))}
                     value]]))]]))


(defn reset-identifiers
  "Assigns a :status of :new to all maps in a collection"
  [identifiers]
  (map (fn [id] (assoc id :status :new)) identifiers))

(defn fetch-organisms [service]
  (im/query-records
   service
   {:from "Organism"
    :select "shortName"}))

(defn swap-organism [state organism]
  (swap! state assoc :extra organism))

(defn swap-type [state value]
  (swap! state assoc :type value))

(defn controls
  "A container for controlling the ID resolution job / proceeding with workflows."
  [state api]
  (fn []
    [:div
     [:div.btn.btn-raised.btn-primary
      {:on-click (fn [e] (handle-values @state api))} "Go"]]))

(defn smartbox
  "Element containing the entire ID resolution."
  [step-data]
  (let [local-state (reagent/atom {:organisms []
                                   :types ["Gene" "Protein"]})
        persistent-state (reagent/atom (merge {:identifiers []
                                               :bank {}
                                               :type "Gene"
                                               :caseSensitive false
                                               :wildCards true
                                               :extra "D. melanogaster"}
                                              (:state step-data)))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:div
                         [:div.row
                          [:div.col-xs-2
                           [:form.form
                            [:div.form-group
                             [:label "Type"]
                             ; Build a dropdown of available organisms
                             [dropdown {:values (:types @local-state)
                                        :value (:type @persistent-state)
                                        :handler (partial swap-type persistent-state)}]]
                            [:div.form-group
                             [:label "Organism"]
                             ; Build a dropdown of available organisms
                             [dropdown {:values (map :shortName (:organisms @local-state))
                                        :value (:extra @persistent-state)
                                        :handler (partial swap-organism persistent-state)}]]]]
                          [:div.col-xs-10
                           [:label "Identifiers"]
                           [:div.smartbox
                            (doall (map (fn [next]
                                          ^{:key (:identifier next)}
                                          [identifier next persistent-state])
                                        (:identifiers @persistent-state)))
                            [input-box persistent-state]]]]

                         [:div.form-group
                         ]
                         ;  [stats (:identifiers @persistent-state)]
                         [controls persistent-state (:api step-data)]
                        ;  (json-html/edn->hiccup @persistent-state)
                        ;  (json-html/edn->hiccup @local-state)
                         ])
      :component-did-mount (fn [this]
                             ; Asynchronously fetch our list of organisms
                             (go (let [organisms (<! (fetch-organisms {:service {:root "beta.flymine.org/beta"}}))]
                                   (swap! local-state assoc :organisms organisms))))})))


(defn ^:export main [step-data]
  "Output a table representing all lists in a mine.
  When the component is updated then inform the API of its new value."
  (let [local-state (reagent/atom {:current-page 1
                                   :rows-per-page 20})]

    (reagent/create-class
     {:reagent-render
      (fn [step-data]
        [:div
         [:h1 "Enter Identifiers"]
         [smartbox step-data]])
      :component-will-receive-props
      (fn [this new-props]
        (let [{:keys [upstream-data api state]} (extract-props new-props)]))})))
