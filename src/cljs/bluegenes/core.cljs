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
              [bluegenes.tools.showlist.core :as showlist]
              [bluegenes.tools.idresolver.core :as idresolver]
              [bluegenes.tools.viewtable.core :as viewtable]
              [bluegenes.tools.runtemplate.core :as runtemplate]
              [intermine.imjs :as imjs]))

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
  (re-frame/dispatch [:load-histories])
  (mount-root))
