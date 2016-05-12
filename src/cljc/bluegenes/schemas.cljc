(ns bluegenes.schemas
  (:require [schema.core :as s]))

(def Payload
  "A schema for sharing data between tools"
  {:data {:format (s/enum "list" "ids" "query")
          :type s/Str
          :payload (s/conditional vector? [s/Num]
                                  map? s/Any
                                  seq? [s/Num]
                                  :else s/Str)}
   :service {:root s/Str
             (s/optional-key :token) s/Str}
   (s/optional-key :shortcut) s/Str})

(def SavedResearch
  {:_id s/Any
   :payload Payload
   :label s/Str
   :structure []
   :when s/Any
   :editing s/Bool
   :count s/Num
   :steps {}
   (s/optional-key :cached-value) s/Any})

