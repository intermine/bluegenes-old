(ns bluegenes.utils.imcljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))



; {:service {:root "http://www.flymine.org/query/"
;            :token "ABCDEF"}}
; {:list "list1"
;  :widget "publication"
;  :maxp 0.05
;  :format "json"
;  :correction "Holm-Bonferroni"}

(defn enrichment
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [ {{:keys [root token]} :service} {:keys [list widget maxp correction]}]
  (go (let [response (<! (http/get (str root "/service/list/enrichment")
                                   {:with-credentials? false
                                    :keywordize-keys? true
                                    :query-params {:list list
                                                   :widget widget
                                                   :maxp maxp
                                                   :format "json"
                                                   :correction correction}}))]
        (-> response :body))))
