(ns bluegenes.tools.viewtable.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [bluegenes.utils.imcljs :as im]
            [intermine.imtables :as imtables]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(defn get-list-query
  "Construct a query using a collection of object ids."
  [payload]
  {:from   (:type payload)
   :select "*"
   :where  [{:path  (:type payload)
             :op    "IN"
             :value (:payload payload)
             :code  "A"}]})

(defn get-id-query
  "Construct a query using an intermine list name."
  [payload]
  {:from   (:type payload)
   :select "*"
   :where  [{:path   (str (:type payload) ".id")
             :values (:payload payload)
             :op     "ONE OF"
             :code   "A"}]})

(defn normalize-input
  "Convert a variety of inputs into an imjs compatible clojure map."
  [input-data]
  (cond
    (= "list" (-> input-data :data :format))
    (get-list-query (get-in input-data [:data]))
    (= "ids" (-> input-data :data :format))
    (get-id-query (get-in input-data [:data]))
    (= "query" (-> input-data :data :format))
    (get-in input-data [:data :payload])))

(defn update-count
  "Reset an atom with the row count of an imjs query."
  [input-data state]
  (let [query (normalize-input input-data)]
    (-> (js/imjs.Service. (clj->js (:service input-data)))
        (.query (clj->js query))
        (.then (fn [q] (.count q)))
        (.then (fn [c]
                 (reset! state c))))))

(defn ^:export preview
  "Render a preview of the tool."
  []
  (let [state (reagent/atom 0)]
    (fn [data]
      (update-count data state)
      [:div
       [:div.heading "View Table"]
       [:div.indented (str @state " rows.")]]
      )))

(defn inner-table
  "Renders an im-table"
  []
  (let [update-table (fn [component]
                       (let [{:keys [state upstream-data api]} (reagent/props component)
                             node (reagent/dom-node component)
                             target (.item (.getElementsByClassName node "imtable") 0)
                             query (if (nil? state)
                                     (normalize-input upstream-data)
                                     state)]

                         (-> (.loadTable js/imtables
                                         target
                                         (clj->js {:start 0 :size 10})
                                         (clj->js {:service (:service upstream-data) :query query}))
                             (.then
                               (fn [table]
                                 (-> table .-history (.on "changed:current"
                                                          (fn [x]
                                                            (println "REPLACING STATE WITH" (-> x .-attributes .-query (.toJSON)))
                                                            ((:replace-state api) (js->clj (-> x .-attributes .-query (.toJSON))
                                                                                           :keywordize-keys true))
                                                            ;(println "about to send out"
                                                            ;         {:format "query"
                                                            ;          :type (-> x .-attributes .-query .-root)
                                                            ;          :payload (js->clj (-> x .-attributes .-query (.toJSON))
                                                            ;                            :keywordize-keys true)})
                                                            ((:has-something api) {:service (:service upstream-data)
                                                                                   :data    {:format  "query"
                                                                                             :type    (-> x .-attributes .-query .-root)
                                                                                             :payload (js->clj (-> x .-attributes .-query (.toJSON))
                                                                                                               :keywordize-keys true)}})
                                                            (.log js/console "changed" x))))

                                 ;(println "ran query" (-> table .-query))
                                 (let [clone (.clone (-> table .-query))
                                       adj (.select clone #js [(str (-> table .-query .-root) ".id")])]
                                   (-> (js/imjs.Service. (clj->js (:service upstream-data)))
                                       (.values adj)
                                       (.then (fn [v]
                                                ;((:has-something api) {:service (:service upstream-data)
                                                ;                       :data {:format "ids"
                                                ;                              :type (-> table .-query .-root)
                                                ;                              :payload (js->clj v)}})
                                                ;(.log js/console "table query" (-> table .-query (.toJSON )))

                                                ((:has-something api) {:service (:service upstream-data)
                                                                       :data    {:format  "query"
                                                                                 :type    (-> table .-query .-root)
                                                                                 :payload (js->clj (-> table .-query (.toJSON))
                                                                                                   :keywordize-keys true)}})

                                                )))))
                               (fn [error]
                                 (println "TABLE HAD AN ERROR" error))))))]

    (reagent/create-class
      {:reagent-render          (fn []
                                  [:div
                                   [:div.imtable]])
       :should-component-update (fn [this old new]
                                  ;(println "SHOULD I UPDATED")
                                  false)
       :component-did-update    update-table
       :component-did-mount     update-table})))


(defn ^:export run
  "This function is called whenever the tool makes a change to its state, or its
  upstream data changes."
  [{:keys [input state cache] :as what-changed}
   {:keys [has-something save-state save-cache]}]
  (println "VIEW TABLE IS RUNNING with input" input)
  (save-state {:service (:service input)
               :data    {:payload (normalize-input input)
                         :format  "query"
                         :type    "Gene"}})
  ;(cond
  ;  (nil? cache)
  ;  (go (let [lists (<! (im/lists {:service {:root "www.flymine.org/query"}}))]
  ;        (save-cache {:lists lists})))
  ;  (contains? state :data)
  ;  (has-something state))

  )


;(defn update-table [component]
;  (let [{:keys [state upstream-data api]} (reagent/props component)
;        node (reagent/dom-node component)
;        target (.item (.getElementsByClassName node "imtable") 0)]
;
;    (-> (.loadTable js/imtables
;                    target
;                    (clj->js {:start 0 :size 10})
;                    (clj->js {:service (:service upstream-data) :query query})))))



(defn somefn [component]
  (let [props (reagent/props component)
        node (reagent/dom-node component)
        target (.item (.getElementsByClassName node "imtable") 0)]
    (println "about to use" (clj->js {:service (:service props) :query (:payload (:data props))}))
    (println "TARGET IS" node)
    (-> (.loadTable js/imtables
                    target
                    (clj->js {:start 0 :size 10})
                    (clj->js {:service (:service props) :query (:payload (:data props))}))
        (.then (fn [table] (println "TABLE" table))
               (fn [error] (println "TABLE ERROR" error))))))

(defn mytable []
  (reagent/create-class
    {:component-did-mount  somefn
     :component-did-update somefn
     :reagent-render       (fn [] [:div [:div.imtable "I AM TABLE"]])}))

(defn ^:export main []
  (fn [{:keys [state]}]
    [mytable state]))

(defn ^:export main2
  "Render the main view of the tool."
  []
  (reagent/create-class
    {:reagent-render          (fn [props]
                                (println "TABLE IS RE RENDERING")
                                [inner-table props])
     :should-component-update (fn [this old-argv new-argv]
                                (println "should component update?")
                                true
                                ;(println "DIFF"
                                ;         (clojure.data/diff (extract-props old-argv)
                                ;                            (extract-props new-argv)))
                                ;(not (=
                                ;       (dissoc (extract-props old-argv) :api :saver :produced)
                                ;       (dissoc (extract-props new-argv) :api :saver :produced)))
                                )}))
