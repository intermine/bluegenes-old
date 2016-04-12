(ns bluegenes.utils.queries)

(defn most-recent-data
  "Returns the data from the most recent step in the current history"
  [db]
  (let [last-in-view (last (get-in db [:histories (:active-history db) :structure]))]
    (get-in db [:histories (:active-history db) :steps last-in-view :produced])))

(defn templates
  "Returns templates from the in-memory database with an optional source keyword."
  [db & [source]]
  (if source
    (get-in db [:cache :templates source])
    (get-in db [:cache :templates])))

(defn models
  "Returns models from the in-memory database with an optional source keyword."
  [db & [source]]
  (if source
    (get-in db [:cache :models source])
    (get-in db [:cache :models])))

(defn mines
  "Returns mines from the in-memory database with an optional source keyword."
  [db & [source]]
  (if source
    (get-in db [:remote-mines source])
    (get-in db [:remote-mines])))
