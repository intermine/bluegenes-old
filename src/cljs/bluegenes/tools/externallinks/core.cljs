(ns bluegenes.tools.externallinks.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]
    [bluegenes.utils.imcljs :as im]
    [bluegenes.tools.externallinks.config :as json-html]
    [reagent.core :as reagent]))

(enable-console-print!)
(def search-results (reagent.core/atom nil))

(defn load-data [upstream-data]
  "Loads links based on config.cljs"
  ;;Need help from julie/rachel to figure out how these identifiers map.
  ;;useful intemrine configs:
  ;;https://github.com/intermine/intermine/blob/8f276ec401e6db779a354dd3c4896671b1082040/bio/webapp/src/org/intermine/bio/web/AttributeLinksController.java
  ;;https://github.com/intermine/intermine/blob/7b379723630cd0e95b30d6f512e90a703418574d/flymine/webapp/resources/web.properties
  )

(defn external-links [local-state api upstream-data]
  [:div
   [:h4 "External links"]])

(defn ^:export main []
  (let [local-state (reagent/atom " ")]
  (reagent/create-class
    {:reagent-render
      (fn render [{:keys [state upstream-data api]}]
        [external-links local-state api upstream-data])
      :component-did-mount (fn [this]
        (let [passed-in-state (:state (reagent/props this))
              api (:api (reagent/props this))
              upstream (:upstream-data (reagent/props this))]
          (reset! local-state (:input passed-in-state)
          ;;don't load homologues for, say, publications
          '(cond (= "Gene" (get-in upstream [:data :type]))
            (load-data upstream)))))
      :component-will-update (fn []
        (reset! search-results nil))
      :component-did-update (fn [this old-props]
        (let [upstream (:upstream-data (reagent/props this))]
          (cond (= "Gene" (get-in upstream [:data :type]))
          (load-data upstream))
          ))})))
