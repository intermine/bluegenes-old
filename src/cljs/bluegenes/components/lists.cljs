(ns bluegenes.components.lists
    (:require [re-frame.core :as re-frame]
      [bluegenes.tools.smartidresolver.core :as idresolver]
      [bluegenes.tools.chooselist.core :as chooselist]
      [bluegenes.timeline.api :as timeline-api]
))

(defn list-upload-section []
  "Quasi-functional ID resolver"
  (let [api (timeline-api/build-api-map {:name "smartidresolver"})]
  [:div.step-container
    [:div.body
     [:div
      [:h3 "Create a new list:"]
      [idresolver/main {:state ["" ""]
        :api api
        :upstream-data nil}]]]]))

(defn list-chooser-section []
  "Quasi-functional ID resolver"
  (let [api (timeline-api/build-api-map {:name "chooselist"})]
  [:div.step-container
    [:div.body
     [:div
      [:h3 "Select an existing list"]
      [chooselist/main {:state ["" ""]
        :api api
        :upstream-data nil}]]]]))

(defn main-view []
  [:main.lists-page
   [:div.cards
      [list-chooser-section]
      [list-upload-section]
    ]
    [:div.step-container
      [:div.body.list-show
       [:div.list
       "All of your list data in a table"
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
      ]]
   ])
