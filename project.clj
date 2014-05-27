(defproject nomad "0.1.0"
  :description "Nomad the game"
  :url "http://github.com/voidco/nomad"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [rm-hull/monet "0.1.10"]
                 [rm-hull/big-bang "0.0.1-SNAPSHOT"]
                 [prismatic/dommy "0.1.2"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :jvm-opts ["-XX:+UseG1GC"]
  :cljsbuild
  {:builds
   {:dev {:source-paths ["src"]
          :compiler {:output-to "resources/public/js/cljs.js"
                     :output-dir "resources/public/js"
                     :optimizations :none
                     :pretty-print true
                     :source-map "resources/public/js/cljs.js.map"}}

    :prod {:source-paths ["src"]
           :compiler {:output-to "resources/public/js-min/cljs-min.js"
                      :output-dir "resources/public/js-min"
                      :optimizations :advanced
                      :pretty-print false
                      :source-map "resources/public/js-min/cljs-min.js.map"}}}})
