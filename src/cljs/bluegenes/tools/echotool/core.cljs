(ns bluegenes.tools.echotool.core
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.impl.util :as impl :refer [extract-props]]
            [intermine.imjs :as imjs]))

(enable-console-print!)

(defn ^:export main []
  "A very simple tool that displays its upstream-data and emits it
  as its output. Used for testing."
  (let [local-state (reagent/atom nil)]
    (reagent/create-class
     {:reagent-render (fn [{:keys [state upstream-data api]}]
                        [:div
                         [:h1 "Echo Tool"]
                         [:h4 "upstream" (str upstream-data)]])
      :component-did-mount (fn [this]
                             (let [props (reagent/props this)]
                              ;  (println "SEES TOOL" (dissoc props :api))
                               ((-> props :api :has-something)
                                {:key (if (nil? (:upstream-data props))
                                        (str "Start of text.")
                                        (str (:key (:upstream-data props))
                                             " +layer"))})))
      :component-did-update (fn [this old-props]
                              ; (println "echo tool updating")
                              (let [props (reagent/props this)]
                                ; (println "SEES TOOL" (dissoc props :api))
                                ((-> props :api :has-something)
                                 {:key (if (nil? (:upstream-data props))
                                         (str "Start of text.")
                                         (str (:key (:upstream-data props))
                                              " +layer"))})))})))
