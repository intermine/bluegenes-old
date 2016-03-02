(ns bluegenes.views
  (:require [re-frame.core :as re-frame]
            [bluegenes.timeline.views :as timeline-views]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.timeline.api :as timeline-api]
            [bluegenes.components.googlesignin :as google-sign-in]
            [bluegenes.tools.idresolver.core :as idresolver]
            [json-html.core :as json-html])
  (:use [json-html.core :only [edn->hiccup]]))

  (defn ui-card [contents header-text]
    "pass homepage elements to ui-card as a function / form-2 component to wrap them in the correct boilerplate html."
    ;TODO: conditional header text item (?)
    [:div.step-container
    [:div.body
    [contents]]])

(defn histories-section []
  [ui-card
  (fn history-card []
  (let [histories (re-frame/subscribe [:all-histories])]
    [:div
     [:h3 "Choose a starting point:"]
     (for [[key values] @histories]
       ^{:key key}
       [:div
        [:a {:href (str "#/timeline/" (:slug values))} [:h4 (:name values)]]
        [:span (:description values)]])]))])

(defn list-upload-section []
  "Nonfunctional (currently) list upload homepage widget"
  (let [api (timeline-api/build-homepage-api-map {:name "idresolver"})]
  [ui-card
   (fn []
     [:div
      [:h3 "I have data I want to know more about:"]
      [idresolver/main {:state " "
        :api api
        :upstream-data nil}]])]))

(defn bubble-section []
  [ui-card
   (fn []
     [:div.bubble
       [:h3 "Explore our data:"]
       [:p "Start by clicking a bubble"]
       [:div [:img {:src "img/bubble.png"}]
      ]])])

(defn templates-section []
  "Outputs the templates 'answer a question' in the homepage"
  ; TODO: make this link to templates. currently not too easy as we don't have a
  ; single gene/protein/organism entry point (I think?). Will need to add the
  ; history details to app db, too
  [ui-card
  (let [templates (re-frame/subscribe [:homepage-template-histories])]
    (fn []
      [:div
        [:h3 "Answer a question:"]
        [:ul.templates
          (for [[key values] @templates]
            ^{:key key}
            [:li
              {:class (:type values)}
              (:description values)]
         )]
       [:a "See other popular questions and template searches..."]
     ]))])

(defn searchbox []
  "Outputs (currently nonfunctional) search box. TODO: replace with keyword search"
  [ui-card
   (fn []
     [:form#search
      [:input {
        :type "text"
        :placeholder "Search for a gene, protein, disease, etc..."}]
      [:button "Search"]])])

(defn home-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:main.homepage
        [searchbox]
        [:div.cards
          [bubble-section]
          [histories-section]
          [list-upload-section]
          [templates-section]
         ]])))

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))

(defn debug-panel []
  (let [state (re-frame/subscribe [:app-state])]
    [:div
     [:h1 "Application State"]
     [:p (json-html/edn->hiccup @state)]]))

(defn nav-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        panel-name (str (clj->js @active-panel))]
    [:nav#custom-bootstrap-menu.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:a.navbar-brand {:href "#"} "βlueGenes"]]
      [:div
       [:ul.nav.navbar-nav
        [:li {:class (if (= panel-name "home-panel") "active")} [:a {:href "#"} "Home"]]
        [:li {:class (if (= panel-name "timeline-panel") "active")} [:a {:href "#/timeline"} "Timeline"]]
        [:li {:class (if (= panel-name "debug-panel") "active")} [:a {:href "#/debug"} "Debug"]]]
       [:div
        [:ul.nav.navbar-nav.pull-right.signin
         [:li
              ;  [google-sign-in/main]
               ]]]]]]))


(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :timeline-panel [] [timeline-views/main-view])
(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div
       [nav-panel]
       (panels @active-panel)
       [dimmer/main]])))
