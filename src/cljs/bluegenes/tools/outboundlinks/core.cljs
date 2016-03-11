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
    :organisms ["H. sapiens"]}
  :mousemine {
        :name "MouseMine"
        :url "http://www.mousemine.org/mousemine"
        :service {:root "http://www.mousemine.org/mousemine/service"}
        :organisms ["M. musculus"]}})

(defn load-data [upstream-data]
  ;(.log js/console "%c Loading data" "border-bottom:skyblue dotted 3px" (clj->js upstream-data))
  (go (let [
      svc (select-keys upstream-data [:service])
      id (get-in upstream-data [:data :payload 0])
      type (get-in upstream-data [:data :type])
      homologues (<! (im/homologues svc (select-keys (:humanmine remote-mines) [:service]) type id (get-in remote-mines [:humanmine :organisms 0])))]
        (swap! search-results assoc :humanmine (first homologues))
    )))

(defn get-identifier [homologue]
  (let [pi (get-in homologue [:Gene :primaryIdentifier])
        symbol (get-in homologue [:homologue :symbol])]
  (if (some? symbol)
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
  "Visual link show component"
  [:div
   [:h5 "Outbound links"]
    (for [[k v] @search-results]
      (let [this-mine (k remote-mines)]
        ^{:key k}
        [:div
         [:div (:name this-mine)]
         [:div (get-in this-mine [:organisms 0])]
         [:div (list-homologues (:homologues v) (:url this-mine))]
       ]))])

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
