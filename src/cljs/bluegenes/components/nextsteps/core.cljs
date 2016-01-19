(ns bluegenes.components.nextsteps.core
  (:require [re-frame.core :as re-frame]))

(defn main []
  (let [dimmer (re-frame/subscribe [:dimmer])]
    (fn []
      [:div.next-steps

      ;  [:h3 "Next Steps"]
      ;  [:button.btn-next-step.btn.btn-default [:i.fa.fa-3x.fa-pie-chart]]
       [:i.fa.fa-3x.fa-pie-chart " "]
       [:i.fa.fa-3x.fa-code-fork " "]
       [:span " "]
       [:i.pull-right.highlight.fa.fa-3x.fa-star]])))
