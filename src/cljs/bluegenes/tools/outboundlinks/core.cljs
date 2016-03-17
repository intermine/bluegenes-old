(ns bluegenes.tools.outboundlinks.core
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
      [:svg.icon.icon-share [:use {:xlinkHref "#icon-share"}]]
      (get-identifier homie)

      ]]) homologues)))

(defn homologue-links [local-state api upstream-data]
  "Visual link show component that shows one result per mine"
  [:div.outbound
  [:h5 "Homologues in other Mines"]
  [:div.homologuelinks
    (let [remote-mines (re-frame/subscribe [:remote-mines])]
      (doall (for [[k v] @search-results]
        (let [this-mine (k @remote-mines)]
          ^{:key k}
          [:div.onemine
           [:h6 (:name this-mine)]
           [:div.subtitle (:organism this-mine)]
           [:div (list-homologues (:homologues v) (:url this-mine))]
         ]))))
   ;;let's tell them we have no homologues if no mines have results,
   ;;but not if it's just because searches haven't come back yet.
   (cond (and
          (some? @search-results)
          (< (count @search-results) 1))
     [:p "No homologues found. :("])]
   ;;handy for debug:
   ;;[:p (json-html/edn->hiccup @search-results)]
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
