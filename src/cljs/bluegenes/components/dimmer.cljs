(ns bluegenes.components.dimmer
  (:require [re-frame.core :as re-frame]))

(defn main []
  (let [dimmer (re-frame/subscribe [:dimmer])]
    (fn []
      [:div#dimmer {:class (if (true? (:active @dimmer)) nil "hide")}
       [:div.message [:div.loader]]])))
