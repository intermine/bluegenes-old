(ns bluegenes.views
  (:require [re-frame.core :as re-frame]
            [bluegenes.timeline.views :as timeline-views]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.googlesignin :as google-sign-in]
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
  (fn []
  (let [histories (re-frame/subscribe [:all-histories])]
    [:div
     [:h4 "Choose a demo history below."]
     (for [[key values] @histories]
       [:div
        [:a {:href (str "#/timeline/" (:slug values))} [:h3 (:name values)]]
        [:span (:description values)]])]))])

(defn list-upload-section []
  [ui-card
   (fn []
     [:div
      [:h4 "I have data I want to know more about"]
      [:p "Upload your list of identifiers (Genes, Proteins, etc.)"]
      [:textarea {:cols 20 :rows 4}]
      [:button "Go!"]])])

(defn templates-section []
  "Outputs the templates 'answer a question' in the homepage"
  ; TODO: make this link to templates. currently not too easy as we don't have a
  ; single gene/protein/organism entry point (I think?). Will need to add the
  ; history details to app db, too
  [ui-card
  (fn []
    [:div
      [:h4 "Answer a question"]
      [:ul.templates
       (let [templates (re-frame/subscribe [:homepage-template-histories])]
        (for [[key values] @templates]
          [:li
            {:class (:type values)}
            (:description values)]
       ))]
     [:a "See other popular questions and template searches..."]
     ])])

(defn searchbox []
  "Outputs (currently nonfunctional) search box"
  [ui-card
   (fn []
     [:form#search
      [:h3 "Search:"]
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
          [histories-section]
          [templates-section]
          [list-upload-section]
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
               [google-sign-in/main]
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
