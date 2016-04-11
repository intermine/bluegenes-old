(ns bluegenes.components.listentry.core
    (:require [re-frame.core :as re-frame]
      [reagent.core :as reagent]
      [bluegenes.tools.smartidresolver.core :as idresolver]
      [bluegenes.tools.chooselist.core :as chooselist]
      [bluegenes.tools.viewtable.core :as viewtable]
      [bluegenes.timeline.api :as timeline-api]
      [json-html.core :as json-html])
      (:use [json-html.core :only [edn->hiccup]]))

(defn is-active? [node-name active-panel]
    (contains? @active-panel node-name)) ;;state doesn't have the selected node class in it

(defn is-inactive? [node-name active-panel]
  (.log js/console node-name @active-panel)
  (and
    (some? @active-panel) ;;state isn't empty
    (not (is-active? active-panel node-name)))) ;;and state doesn't have the selected node class in it

(defn activate-panel [e this-node active-panel]
  (.log js/console "activating" e this-node active-panel)
  (let [container (.-currentTarget e)]
     (reset! active-panel (set (array-seq (.-classList container))))
    ))

(defn list-upload-section [active-panel]
  "Quasi-functional ID resolver"
  (let [api (timeline-api/build-list-entry-api-map {:name "smartidresolver"})
        this-node "list-upload-section"]
  [:div.step-container
    {:on-click (fn [e]
      (activate-panel e this-node active-panel))
     :class (str this-node
       (cond
         (is-active? this-node active-panel) " active"
         (is-inactive? this-node active-panel) " inactive"))
     }
    [:div.body
     [:div
      [:h3 "Create a new list:"]
      [:div.tool [idresolver/main {:state ["" ""]
        :api api
        :upstream-data nil}]]]]]))

(defn list-chooser-section [active-panel]
  "Quasi-functional ID resolver"
  (let [api (timeline-api/build-list-entry-api-map {:name "chooselist"})
        this-node "list-chooser-section"]
  [:div.step-container
   {:on-click (fn [e]
     (activate-panel e this-node active-panel))
    :class (str this-node
      (cond
        (is-active? this-node active-panel) " active"
        (is-inactive? this-node active-panel) " inactive"))
    }
    [:div.body
     [:div
      [:h3 "Select an existing list"]
      [:div.tool [chooselist/main {:state ["" ""]
        :api api
        :upstream-data nil}]]]]]))

(defn main-view []
  (let [
    active-panel (reagent/atom nil)
    api (timeline-api/build-api-map {:name "viewtable"})
    upstream-data (re-frame/subscribe [:list-entry-data])
]

(.log js/console "upstream" (clj->js @upstream-data))

  [:main.lists-page
   [:div.cards
      [list-chooser-section active-panel]
      [list-upload-section active-panel]
    ]
    [:div.step-container
      [:div.body.list-show
       [:div.list
       "Active" (edn->hiccup @active-panel)
       [viewtable/main
        {:state []
        :api api
        :upstream-data (first @upstream-data)}]
        ]
       [:div.toolbox
        [:div
         [:h5 "Toolbox"]
          [:p [:svg.icon
            [:use {:xlinkHref "#icon-info"}]]
            "If the list looks right to you, analyse it to learn more or save it for later: "]

          [:button "List analysis ->"]]
          [:button
            [:svg.icon.icon-floppy-disk
              [:use {:xlinkHref "#icon-floppy-disk"}]]
                " Save list"]]
;         (edn->hiccup upstream-data)
      ]]
   ]))
