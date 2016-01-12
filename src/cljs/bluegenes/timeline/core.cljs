(ns bluegenes.timeline.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [bluegenes.timeline.handlers]
            [bluegenes.timeline.subs]
            [bluegenes.timeline.views :as views]
            [bluegenes.config :as config]))

(enable-console-print!)

;(.log js/console "TIMELINE CORE LOADED" bluegenes.tools)
;(re-frame/dispatch [:activate-dimmer "please wait"])

