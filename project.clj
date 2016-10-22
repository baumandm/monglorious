(defproject monglorious "0.1.0-SNAPSHOT"
  :description "Query MongoDB using strings!"
  :url "https://github.com/baumandm/monglorious"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot monglorious.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
