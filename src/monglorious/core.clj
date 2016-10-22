(ns monglorious.core
  (:gen-class)
  (:require [instaparse.core :as insta]))

(def whitespace
  (insta/parser
    "whitespace = #'\\s+'"))

(def monglorious-parser
  (time (insta/parser (clojure.java.io/resource "grammar.ebnf")
                      :string-ci true
                      :auto-whitespace whitespace)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
