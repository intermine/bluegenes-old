(defproject bluegenes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]
                 [re-frame "0.6.0"]
                 [secretary "1.2.3"]
                 [compojure "1.4.0"]
                 [ring "1.4.0"]
                 [json-html "0.3.6"]
                 [reagent-forms "0.5.8"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-2"]
            [lein-doo "0.1.6"]
            [lein-less "1.7.5"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]
  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"}

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler bluegenes.handler/handler}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "bluegenes.core/mount-root"}
                        :compiler {:main bluegenes.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :foreign-libs [{:file "foreign-libs/im.min.js"
                                                   :provides ["intermine.imjs"]}]
                                   }}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main bluegenes.runner
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main bluegenes.core
                                   ;:output-dir "resources/public/js/compiled"
                                   :output-to "resources/public/js/compiled/prod.js"
                                   ;:source-map "resources/public/js/compiled/prod.js.map"
                                   :externs ["externs/tools.js"]
                                   :foreign-libs [{:file "foreign-libs/im.min.js"
                                                   :provides ["intermine.imjs"]}]
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
