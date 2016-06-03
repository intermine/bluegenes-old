(defproject bluegenes "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.5.1"]
                 [re-frame "0.6.0"]
                 [cljsjs/react "0.14.3-0"] ;;this React lib allow svgs to render properly. Remove if Reagent ever uses React > 0.14
                 [secretary "1.2.3"]
                 [compojure "1.4.0"]
                 [cheshire "5.5.0"]
                 [ring/ring-json "0.2.0"]
                 [ring "1.4.0"]
                 [json-html "0.3.6"]
                 [environ "1.0.0"]
                 [mount "0.1.9"]
                 [prismatic/schema "1.0.5"]
                 [slugger "1.0.1"]
                 [funcool/cuerdas "0.7.0"]
                 [reagent-forms "0.5.8"]
                 [clj-http "2.0.1"]
                 [com.taoensso/timbre "4.1.0"]
                 [cljs-ajax "0.5.3"]
                 [com.novemberain/monger "3.0.2"]
                 [org.clojure/core.async "0.2.374"]
                 [prismatic/dommy "1.1.0"]
                 [cljs-http "0.1.39"]
                 [com.rpl/specter "0.10.0"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [binaryage/devtools "0.6.1"]]

  :source-paths ["src/clj"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler bluegenes.handler/app}

  :main bluegenes.handler

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "test/js"]

  :test-paths ["test/clj"]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-3"]
            [lein-environ "1.0.0"]
            [lein-shell "0.5.0"]
            [lein-less "1.7.5"]]

  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"}

  :min-lein-version "2.5.0"

  :uberjar-name "bluegenes.jar"

  :aliases {"foreign" ["do"
                       ["shell" "curl" "-o" "foreign-libs/imtables.min.js" "http://cdn.intermine.org/js/intermine/im-tables/2.0.0/imtables.min.js"]
                       ["shell" "curl" "-o" "foreign-libs/im.min.js" "http://cdn.intermine.org/js/intermine/imjs/3.15.0/im.min.js"]]}

  :profiles {
             :dev {:cljsbuild {:builds
                               {:dev {:source-paths ["src/cljc" "src/cljs"]
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
                                :min {:source-paths ["src/cljc" "src/cljs"]
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
