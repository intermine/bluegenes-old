(ns bluegenes.utils)

(defn truncate-vector-to-value
  "Returns a trimed vector from its beginning to the location of value.
  It supports vectors nested one deep and returns the outer most values.
  Example:
  (truncate-haystack [:a :b :c [:d :e :f :g] :h :i] :f)
  clj.user=> [:a :b :c [:d :e :f :g]"
  [haystack needle]
  (loop [pos 0
         new-haystack []]
    (if (< pos (count haystack))
      (let [item (nth haystack pos)]
        (if (if (vector? item)
              (not (nil? (some #{needle} item)))
              (= needle item))
          (conj new-haystack item)
          (recur (inc pos) (conj new-haystack item))))
      haystack)))

(defn diff [s1 s2]
  "Subtract one list (s2) from another (s1)."
  (mapcat
   (fn [[x n]] (repeat n x))
   (apply merge-with - (map frequencies [s1 s2]))))
