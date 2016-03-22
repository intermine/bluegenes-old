(ns bluegenes.tools.minelinks.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]
    [bluegenes.utils.imcljs :as im]
    [json-html.core :as json-html]
    [reagent.core :as reagent]))

(enable-console-print!)
(def search-results (reagent.core/atom nil))

(defn load-data [upstream-data]
  "Loads homologues from each mine."
  (let [remote-mines (re-frame/subscribe [:remote-mines])]
    (doall (for [[minename details] @remote-mines]
      (go (let [
        svc (select-keys upstream-data [:service])
        id (get-in upstream-data [:data :payload 0])
        type (get-in upstream-data [:data :type])
        homologues (<! (im/homologues svc (select-keys details [:service]) type id (get-in details [:organism])))]
            (.log js/console "Homologues for" (clj->js minename) (clj->js homologues))
          (swap! search-results assoc minename homologues)
      ))))))

(defn get-identifier [homologue]
  "returns an identifier. looks for the symbol first, if there is one, or otherwise uses the primary identifier."
  (let [pi (get-in homologue [:homologue :primaryIdentifier])
        symbol (get-in homologue [:homologue :symbol])]
  (if symbol
    symbol
    pi)
))

(defn list-homologues [homologues url]
  "Visual component. Given a list of homologues as an IMJS result, output all in a list format"
  (into [:ul.homologues] (map (fn [homie]
    [:li
     [:a {
        :href (str url "/report.do?id=" (get-in homie [:homologue :objectId]))
        :target "_blank"}
      [:svg.icon.icon-external [:use {:xlinkHref "#icon-external"}]]
      (get-identifier homie)
      ]]) homologues)))

(defn status-no-known-homologues [empty-mines remote-mines]
  "outputs visual list of mines for which we have 0 homologue results"
  [:div.no-homologues
   "No known homologues found in: "
    (doall
      (map (fn [mine]
        (:name (mine @remote-mines))) empty-mines))])

(defn status-waiting-for-homologues [remote-mines]
  "Visually output mine list for which we still have no results."
  [:div.awaiting-homologues
   [:svg.icon.icon-waiting [:use {:xlinkHref "#icon-waiting"}]]
   "Awaiting results from: "
    (doall
      (for [[k v] @remote-mines]
        (cond (nil? (k @search-results))
        ^{:key k}
        [:span (:name v)])))])

(defn status-list []
  "Give the user status of mines for which we are still loading or have no results for"
  (let [remote-mines (re-frame/subscribe [:remote-mines])
        mine-names (set (keys @remote-mines))
        active-mines (set (keys @search-results))
        waiting-mines (clojure.set/difference mine-names active-mines)
        empty-mines (keys (filter (fn [[k v]] (empty? (:homologues v))) @search-results))
        ]
    [:div.status-list
     ;(.log js/console "all" (clj->js mine-names) "active" (clj->js active-mines) "waiting" (clj->js waiting-mines) "Empty:" (clj->js empty-mines))
      ;;output mines from which we're still awaiting results
      (cond (seq waiting-mines)
        [status-waiting-for-homologues remote-mines])
      ;;output mines with 0 results.
      (cond (seq empty-mines)
        [status-no-known-homologues empty-mines remote-mines])]))


(defn successful-homologue-results []
  "visually outputs results for each mine that has more than 0 homologues."
  (let [remote-mines (re-frame/subscribe [:remote-mines])]
    [:div.homologuelinks
    (doall (for [[k v] @search-results]
      (let [this-mine (k @remote-mines)
            homies (:homologues v)]
        (if (> (count homies) 0)
          ;;Output successful mines
          (doall
            ^{:key k}
            [:div.onemine
              [:h6 (:name this-mine)]
              [:div.subtitle (:organism this-mine)]
              [:div (list-homologues homies (:url this-mine))]])
       ))))]))


(defn homologue-links [local-state api upstream-data]
  "Visual link show component that shows one result per mine"
  [:div.outbound
    [:h5 "Homologues in other Mines"]
    (if (some? @search-results)
      ;;if there are results
      [:div
        [status-list]
        [successful-homologue-results]
        ;;let's tell them we have no homologues if no mines have results,
        ;;but not if it's just because searches haven't come back yet.
        (cond (< (count @search-results) 1)
          [:p "No homologues found. "])]
      ;;if there are no results
      [:div.disabled
        [:svg.icon.icon-external [:use {:xlinkHref "#icon-question"}]]
       " Want to see homologues? Search for something above, then select a search result to see more details."])

      ])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [homologue-links local-state api upstream-data])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))
              api (:api (reagent/props this))
              upstream (:upstream-data (reagent/props this))]
          (reset! local-state (:input passed-in-state)
          ;;don't load homologues for, say, publications
          (cond (= "Gene" (get-in upstream [:data :type]))
            (load-data upstream)))))
      :component-will-update (fn []
        (reset! search-results nil))
      :component-did-update (fn [this old-props]
        (let [upstream (:upstream-data (reagent/props this))]
          (cond (= "Gene" (get-in upstream [:data :type]))
          (load-data upstream))
          ))})))
