(ns bluegenes.components.homecircles.core
  (:require [re-frame.core :as re-frame]))

(defn searchbox []
[:div.search
;  [search/main]

  [:div.info
   [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
   " Search for genes, proteins, pathways, ontology terms, authors, etc."]])

(defn lists []
  [:div.feature.lists
  [:h3 "Lists"]
   [:div.piccie [:a {:href "#/timeline/project1/network1"} [:svg.icon.icon-summary [:use {:xlinkHref "#icon-summary"}]]] ]
    [:div [:a {:href "#/timeline/project1/network1"} "View"]
    [:a {:href "#/timeline/project1/network1"} "Upload"]]
   ])


 (defn templates []
   [:div.feature.templates
   [:h3 "Templates"]
    [:div.piccie [:a {:href "#/timeline/project1/network1"} [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]] ]]
     [:div [:a {:href "#/timeline/project1/network1"} "Browse"]]
    ])

(defn help []
  [:div.feature.help
  [:h3 "Help"]
   [:div.piccie [:a {:href "#/timeline/project1/network1"} [:svg.icon.icon-summary [:use {:xlinkHref "#icon-eh"}]]] ]
    [:div [:a {:href "#/timeline/project1/network1"} "Tour"]
    [:a {:href "#/timeline/project1/network1"} "Docs/Help"]]
   ])


(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
       ;[searchbox]
       [:div.features
        [lists]
        [templates]
        [help]


       ])))
