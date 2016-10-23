(defproject monglorious "0.1.0-SNAPSHOT"
  :author "Dave Bauman"
  :description "Query MongoDB using strings!"
  :url "https://github.com/baumandm/monglorious"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [instaparse "1.4.3"]
                 [com.novemberain/monger "3.1.0"]
                 [org.clojars.frozenlock/commons-lang "3.3.0"]]

  :main ^:skip-aot monglorious.core
  :target-path "target/%s"

  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.2.1"]]}
             :uberjar {:aot :all}})
