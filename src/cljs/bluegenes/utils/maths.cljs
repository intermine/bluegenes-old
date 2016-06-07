(ns bluegenes.utils.maths)

(defn circle-intersections
  "Determines the pair of X and Y coordinates where two circles intersect."
  [x0 y0 r0 x1 y1 r1]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        d (.sqrt js/Math (+ (* dy dy) (* dx dx)))
        a (/ (+ (- (* r0 r0) (* r1 r1)) (* d d)) (* 2 d))
        x2 (+ x0 (* dx (/ a d)))
        y2 (+ y0 (* dy (/ a d)))
        h (.sqrt js/Math (- (* r0 r0) (* a a)))
        rx (* (* -1 dy) (/ h d))
        ry (* dx (/ h d))
        xi (+ x2 rx)
        xi-prime (- x2 rx)
        yi (+ y2 ry)
        yi-prime (- y2 ry)]
    [xi xi-prime yi yi-prime]))