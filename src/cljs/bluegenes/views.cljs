(ns bluegenes.views
  (:require [re-frame.core :as re-frame]
            [bluegenes.timeline.views :as timeline-views]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.components.googlesignin :as google-sign-in]
            [json-html.core :as json-html])
  (:use [json-html.core :only [edn->hiccup]]))

(defn histories-section []
  (let [histories (re-frame/subscribe [:all-histories])]
    [:div
     [:h4 "Choose a demo history below."]
     (for [[key values] @histories]
       [:div
        [:a {:href (str "#/timeline/" (:slug values))} [:h3 (:name values)]]
        [:span (:description values)]])]))

(defn home-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.step-container
       [:div.step-inner
        [:div.body
         [histories-section]]
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
        [:ul.nav.navbar-nav.pull-right
         [:li [:a
               [google-sign-in/main]
               ]]]]]]]))


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
