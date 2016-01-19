(ns bluegenes.components.nextsteps.core
  (:require [re-frame.core :as re-frame]))

(defn main []
  (let [dimmer (re-frame/subscribe [:dimmer])]
    (fn []
      [:div.next-steps
       [:h3 "Next Steps"]])))
