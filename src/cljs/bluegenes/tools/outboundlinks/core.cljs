(ns bluegenes.tools.outboundlinks.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]
    [bluegenes.utils.imcljs :as im]
    [reagent.core :as reagent]))

(enable-console-print!)
(def search-results (reagent.core/atom nil))

(def remote-mines {
  :humanmine {
    :name "HumanMine"
    :url "http://www.humanmine.org/humanmine"
    :service {:root "http://www.humanmine.org/humanmine/service"}
    :organism "H. sapiens"}
  :yeastmine {
    :name "YeastMine"
    :url "http://yeastmine.yeastgenome.org/yeastmine"
    :service {:root "http://yeastmine.yeastgenome.org/yeastmine/service"}
    :organism "S. cervisiae"}
  :zebrafishmine {
    :name "ZebraFishMine"
    :url "http://www.zebrafishmine.org"
    :service {:root "http://www.zebrafishmine.org"}
    :organism "D. rerio"}
  :mousemine {
    :name "MouseMine"
    :url "http://www.mousemine.org/mousemine"
    :service {:root "http://www.mousemine.org/mousemine/service"}
    :organism "M. musculus"}})

(defn load-data [upstream-data]
  "Loads one data "
  (doall (for [[minename details] remote-mines]
    ;(.log js/console "%c Remotes" "border-bottom:skyblue dotted 3px" (clj->js details)
    ;)
    (go (let [
      svc (select-keys upstream-data [:service])
      id (get-in upstream-data [:data :payload 0])
      type (get-in upstream-data [:data :type])
      homologues (<! (im/homologues svc (select-keys details [:service]) type id (get-in details [:organism])))]
        (swap! search-results assoc minename (first homologues))
    )))))

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
        :href(str url "/report.do?id=" (get-in homie [:homologue :objectId]))}
      (get-identifier homie)]]) homologues)))

(defn homologue-links [local-state api upstream-data]
  "Visual link show component that shows one result per mine"
  [:div.outbound
  [:h5 "Homologues in other Mines"]
  [:div.homologuelinks
    (for [[k v] @search-results]
      (let [this-mine (k remote-mines)]
        ^{:key k}
        [:div.onemine
         [:h6 (:name this-mine)]
         [:div.subtitle (:organism this-mine)]
         [:div (list-homologues (:homologues v) (:url this-mine))]
       ]))]])

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
          (.log js/console "================" (clj->js upstream))
          (reset! local-state (:input passed-in-state)
          ;;don't load homologues for, say, publications
          (cond (= "Gene" (get-in upstream [:data :type]))
            (load-data upstream)))))
      :component-did-update (fn [this old-props]
        (let [upstream (:upstream-data (reagent/props this))]
          (cond (= "Gene" (get-in upstream [:data :type]))
          (load-data upstream))
          ))})))
