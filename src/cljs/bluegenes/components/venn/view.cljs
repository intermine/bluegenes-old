(ns bluegenes.components.venn.view
  (:require [re-frame.core :as re-frame]
    ;[bluegenes.components.venn.handlers]
    ;[bluegenes.components.venn.subs]
            [bluegenes.utils.maths :as maths]
            [reagent.core :as reagent]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(def width 500)
(def height 500)

(def anchor1 {:x (- (* .33 width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 width) (/ width 2)) :y 0})
(def radius (* .33 width))

(println "anchor2" anchor2)

(def center {:x 250 :y 250})
;(def radius 150)


(defn overlap []
  (let [intersection-points (maths/circle-intersections (:x anchor1) 0 radius (:x anchor2) 0 radius)]
    [:path.overlap {:d (clojure.string/join
                         " "
                         ["M" (nth intersection-points 1) (nth intersection-points 3)
                          "A" radius radius 0 0 1
                          (nth intersection-points 0) (nth intersection-points 2)
                          "A" radius radius 0 0 1
                          (nth intersection-points 1) (nth intersection-points 3)
                          "Z"])}]))

(defn circle2 []
  (let [representing (re-frame/subscribe [:qop-2])]
    (fn []
      [:circle.circle2
       {:on-click #(re-frame/dispatch [:toggle-qop 2])
        :class    (if (:keep @representing) "on")
        :r        radius}])))

(defn circle1 []
  (let [representing (re-frame/subscribe [:qop-1])]
    (fn []
      [:circle.circle1
       {:on-click #(re-frame/dispatch [:toggle-qop 1])
        :class    (if (:keep @representing) "on")
        :r        radius}])))

(defn svg []
  (fn []
    [:svg.venn {:width width :height height}
     [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
      [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circle1]]
      [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circle2]]
      [overlap]
      ]]))

(defn main []
  (fn []
    [:div
     [:h1 "VENN"]
     [svg]]))

