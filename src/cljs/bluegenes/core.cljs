(ns bluegenes.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [bluegenes.handlers]
              [bluegenes.subs]
              [bluegenes.routes :as routes]
              [bluegenes.views :as views]
              [bluegenes.config :as config]
              [bluegenes.timeline.core]
              [bluegenes.tools.chooselist.core :as chooselist]
              [bluegenes.tools.idresolver.core :as idresolver]
              [bluegenes.tools.viewtable.core :as viewtable]
              [bluegenes.tools.smartidresolver.core :as smartidresolver]
              [bluegenes.tools.search.core :as search]
              [bluegenes.tools.summary.core :as summary]
              [bluegenes.tools.minelinks.core :as minelinks]
              [bluegenes.tools.externallinks.core :as externallinks]
              [bluegenes.tools.runtemplate.core :as runtemplate]
              [bluegenes.tools.echotool.core :as echotool]
              [bluegenes.tools.chooselistcompact.core :as chooselistcompact]
              [bluegenes.tools.enrichment.core :as enrichment]
              [bluegenes.tools.dashboard.core :as dashboard]
              [bluegenes.tools.templatechooser.core :as templatechooser]
              [bluegenes.tools.datamerger.core :as datamerger]
              [intermine.imjs :as imjs]
              [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(when config/debug?
  (.log js/console "dev mode"))

(def flymine (js/imjs.Service. #js {:root "www.flymine.org/query"}))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:bootstrap-app])  (re-frame/dispatch [:load-histories])
  (re-frame/dispatch [:run-step [:projects :project1 :networks :network1 :nodes :node1]])
  (mount-root))
