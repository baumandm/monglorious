(defproject monglorious "0.8.0"
  :author "Dave Bauman"
  :description "Query MongoDB using strings!"
  :url "https://github.com/baumandm/monglorious"
  :license {:name         "Eclipse Public License"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [instaparse "1.4.9"]
                 [com.novemberain/monger "3.1.0"]
                 [org.clojars.frozenlock/commons-lang "3.3.0"]
                 [clj-time "0.13.0"]]

  :target-path "target/%s"
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :profiles {:dev     {:dependencies [[midje "1.9.4"]
                                      [org.slf4j/slf4j-nop "1.7.25"]]
                       :plugins      [[lein-midje "3.2.1"]
                                      [lein-kibit "0.1.3"]
                                      [jonase/eastwood "0.2.3"]
                                      [lein-codox "0.10.3"]]}

             :uberjar {:aot :all}}

  :codox {:namespaces [monglorious.core monglorious.parser]}
  :release-tasks [["vcs" "assert-committed"]
                  ["clean"]
                  ["kibit"]
                  ["midje"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]]
  )
