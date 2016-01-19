(defproject bluegenes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.5.1"]
                 [re-frame "0.6.0"]
                 [secretary "1.2.3"]
                 [compojure "1.4.0"]
                 [ring "1.4.0"]
                 [json-html "0.3.6"]
                 [environ "1.0.0"]
                 [reagent-forms "0.5.8"]]

  :source-paths ["src/clj"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler bluegenes.handler/handler}

  :main bluegenes.handler

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :test-paths ["test/clj"]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-3"]
            [lein-environ "1.0.0"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "bluegenes.jar"

  :aliases {"foreign" ["do"
                       ["shell" "wget" "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/imtables.min.js" "-O" "foreign-libs/imtables.min.js"]
                       ["shell" "wget" "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js" "-O" "foreign-libs/im.min.js"]]}

  :profiles {
             :dev {:cljsbuild {:builds
                               {:dev {:source-paths ["src/cljs"]
                                      :figwheel {:on-jsload "bluegenes.core/mount-root"}
                                      :compiler {:main bluegenes.core
                                                 :output-to "resources/public/js/compiled/app.js"
                                                 :output-dir "resources/public/js/compiled/out"
                                                 :asset-path "js/compiled/out"
                                                 :source-map-timestamp true
                                                 :foreign-libs [{:file "foreign-libs/im.min.js"
                                                                 :provides ["intermine.imjs"]}
                                                                {:file "foreign-libs/imtables.min.js"
                                                                 :provides ["intermine.imtables"]}]}}
                                :min {:source-paths ["src/cljs"]
                                      :compiler {:main bluegenes.core
                                                 :output-to "resources/public/js/compiled/app.js"
                                                 :externs ["externs/bluegenes.js"]
                                                 :foreign-libs [{:file "foreign-libs/im.min.js"
                                                                 :provides ["intermine.imjs"]}
                                                                {:file "foreign-libs/imtables.min.js"
                                                                 :provides ["intermine.imtables"]}]
                                                 :optimizations :advanced
                                                 :closure-defines {goog.DEBUG false}
                                                 :pretty-print false}}}}}

             :uberjar {
                       :hooks [leiningen.cljsbuild]
                      ;  :prep-tasks ["compile" ["cljsbuild" "once" "prod"]]
                       :main bluegenes.handler
                       :cljsbuild {:builds {:prod
                                            {:source-paths ["src/cljs"]
                                             :compiler {:main bluegenes.core
                                                        :output-to "resources/public/js/compiled/app.js"
                                                        :externs ["externs/bluegenes.js"]
                                                        :foreign-libs [{:file "foreign-libs/im.min.js"
                                                                        :provides ["intermine.imjs"]}
                                                                       {:file "foreign-libs/imtables.min.js"
                                                                        :provides ["intermine.imtables"]}]
                                                        :optimizations :advanced
                                                        :closure-defines {goog.DEBUG false}
                                                        :pretty-print false}}}}}})
