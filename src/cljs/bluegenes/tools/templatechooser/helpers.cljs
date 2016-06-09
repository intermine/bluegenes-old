(ns bluegenes.tools.templatechooser.helpers
  (:require [bluegenes.utils.imcljs :as im]))

(defn with-constraint-class
  "Filter templates for those with a constraint on a particular class."
  [model templates class]
  (filter (fn [[_ {where :where}]]
            (some? (some (fn [{path :path}] (= class (im/end-class model path))) where)))
          templates))

(defn with-one-editable-constraint
  "Filter templates for those with only one constraint."
  [templates]
  (filter (fn [[_ {where :where}]]
            (= 1 (count (filter (fn [c] (= true (:editable c))) where)))) templates))

(defn replace-input-constraints
  [model query class-to-replace new-value]
  (update-in query [:where]
             (fn [constraints]
               (map (fn [constraint]
                      (if (and (= class-to-replace (im/end-class model (:path constraint)))
                               (= true (:editable constraint)))
                        (-> (dissoc constraint :value)
                            (assoc :path (str (im/trim-path-to-class model (:path constraint)) ".id")
                                   :op "ONE OF"
                                   :values new-value))
                        constraint)) constraints))))

(defn replace-input-constraints-whole
  [model query class-to-replace new-value]
  (update-in query [:where]
             (fn [constraints]
               (map (fn [constraint]
                      (if (and (= class-to-replace (im/end-class model (:path constraint)))
                               (= true (:editable constraint)))
                        (assoc new-value :path (:path constraint))
                        constraint)) constraints))))

(def runnable (comp with-one-editable-constraint with-constraint-class))