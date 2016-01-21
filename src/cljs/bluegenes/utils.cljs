(ns bluegenes.utils)

(defn diff [s1 s2]
  "Subtract one list (s2) from another (s1)."
  (mapcat
    (fn [[x n]] (repeat n x))
    (apply merge-with - (map frequencies [s1 s2]))))
