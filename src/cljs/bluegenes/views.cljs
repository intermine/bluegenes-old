(ns bluegenes.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [bluegenes.timeline.views :as timeline-views]
            [bluegenes.components.dimmer :as dimmer]
            [bluegenes.timeline.api :as timeline-api]
            [bluegenes.components.listentry.core :as lists]
            [bluegenes.components.homecircles.core :as circles]
            [clojure.string :as str]
            [bluegenes.utils.icons :as icons]
    ; [bluegenes.components.googlesignin :as google-sign-in]
            [bluegenes.tools.smartidresolver.core :as idresolver]
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
          (cond key (do                                     ;;there's a nil key coming from somewhere that spawns lots of console errors. This cond prevents the errors.
                      ^{:key key}
                      [:div
                       [:h4 [:a {:href (str "#/timeline/" (:slug values))} (:name values)]]
                       [:span (:description values)]])))]))])

(defn list-upload-section []
  "Nonfunctional (currently) list upload homepage widget"
  (let [api (timeline-api/build-homepage-api-map {:name "smartidresolver"})]
    [ui-card
     (fn []
       [:div
        [:h3 "I have data I want to know more about:"]
        [idresolver/main {:state         ["" ""]
                          :api           api
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

(defn submit-search [event state]
  "prevents default behaviours and navigates the user to the search page, with the search input as a query param (trimmed)"
  (.preventDefault js/event)
  (.log js/console "Setting global search term to:" @state)
  (re-frame/dispatch [:set-search-term (str/trim @state)])  ;;set global search
  (aset js/window "location" "href"
  ;;temporarily disable search because it's broken
    ;"/#timeline/search"))
  "#/timeline/project1/network1"))

(defn searchbox []
  "Outputs main top search boc"
  [ui-card
   (let [local-state (reagent/atom nil)]
     (reagent/create-class
       {:reagent-render
        (fn []
          [:form#search {
                         :on-submit (fn [event] (submit-search event local-state))
                         :method    "get"
                         ;;search is broken. So just point to the workflow 'til we fix it.
;                         :action    "/#/timeline/search"}
                         :action    "#/timeline/project1/network1"}
           [:input {
                    :type        "text"
                    :value       (cond (some? @local-state) @local-state)
                    :placeholder "Search for a gene, protein, disease, etc..."
                    :on-change   (fn [val]
                                   (reset! local-state (-> val .-target .-value)))}]
           [:button "Search"]
           ])}))])

(defn home-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:main.home.circles
        [circles/main-panel]
        [:br]
        [:div.temp "Longer term, this page will look more like "
          [:a {:href "https://drive.google.com/file/d/0B5MwfbL4xkvrR1ZkMlR4VUxkMGM/view?usp=sharing"} "this"]
         " or "
          [:a {:href "https://drive.google.com/file/d/0B5MwfbL4xkvrRkhUSmd3RXJQTXM/"} "this"]
         ". Please help us decide which by "
         [:a {:href "https://yochannah.typeform.com/to/dRVLkz"} "voting"]
         " on it."]
       ;[searchbox]
       ])))

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))

(defn debug-panel []
  (let [state (re-frame/subscribe [:app-state])]
    [:div
     [:h1 "Application State"]
     [:div (json-html/edn->hiccup (dissoc @state :cache))]]))

(defn navbar-search []
  "Very similar to the main homepage search but shows on every page and has slightly different markup"
  (let [local-state (reagent/atom "")]
    (reagent/create-class
      {:reagent-render
       (fn []
         [:form.navbar-form
          {:role      "search"
           :on-submit (fn [event] (submit-search event local-state))}
          [:input.form-control
           {:placeholder "Search"
            :type        "text"
            :value       @local-state
            :on-change   (fn [val]
                           (reset! local-state (-> val .-target .-value)))}]
          [:button.btn.btn-default {:type "submit"}
           [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]])})))


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
        ;;uncomment this when list entry points are more polished. they can still be accessed via /lists directly.
        ;        [:li {:class (if (= panel-name "list-panel") "active")} [:a {:href "#/lists"} "Lists"]]
        ;;don't show timeline in navbar unless we're actually there already, as
        ;clicking on timeline from elsewhere just gives a blank page
        (if (= panel-name "timeline-panel")
          [:li {:class (if (= panel-name "timeline-panel") "active")} [:a {:href "#/timeline"} "Timeline"]])
        ;;this isn't much use for users, just devs. uncomment it if you must for heavy debug sessions.
        [:li {:class (if (= panel-name "debug-panel") "active")} [:a {:href "#/debug"} "Debug"]]
        ]
       [:div
        [:ul.nav.navbar-nav.navbar-right                    ;.signin
         [:li
          [navbar-search]]
         [:li
          ;  [google-sign-in/main]
          ]]]]]]))


(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :timeline-panel [] [timeline-views/main-view])
(defmethod panels :operations-panel [] [timeline-views/operations])
(defmethod panels :saved-data-panel [] [timeline-views/saved-data-view])
(defmethod panels :list-panel [] [lists/main-view])
(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.approot
       [icons/icons]
       [nav-panel]
       (if (vector? @active-panel)
         (panels (first @active-panel) (rest @active-panel))
         (panels @active-panel))

       [dimmer/main]
       ])))
