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
   :source s/Keyword
   (s/optional-key :shortcut) s/Str})
