(def source-paths ["src/main/cljs"])
(def version (get (System/getenv) "REACT_CLJS_MODAL_VERSION" "master-SNAPSHOT"))

(defproject org.broadinstitute/react-cljs-modal version
  :dependencies
  [
   [dmohs/react "1.1.0+15.4.2-2"]
   [frankiesardo/linked "1.2.9"]
   ]
  :plugins [[lein-cljsbuild "1.1.5"] [lein-figwheel "0.5.10"]]
  :profiles {:ui
             {:dependencies [[binaryage/devtools "0.9.4"]
                             [org.clojure/clojure "1.8.0"]
                             [org.clojure/clojurescript "1.9.521"]]
              :target-path "resources/public/target"
              :clean-targets ^{:protect false} ["resources/public/target"]
              :cljsbuild
              {:builds
               {:client
                {:source-paths ~(concat source-paths ["src/test/cljs"])
                 :compiler
                 {:main "webui.main"
                  :optimizations :none
                  :source-map true
                  :source-map-timestamp true
                  :output-dir "resources/public/target/build"
                  :output-to "resources/public/target/compiled.js"
                  :asset-path "target/build"
                  :preloads [devtools.preload]
                  :external-config {:devtools/config {:features-to-install [:formatters :hints]}}}
                 :figwheel true}}}}}
  :source-paths ~source-paths
  :cljsbuild {:builds {:client {:source-paths ~source-paths}}}
  :deploy-repositories [["clojars" {:sign-releases false}]])
