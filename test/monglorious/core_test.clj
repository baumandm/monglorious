(ns monglorious.core-test
  (:require [monglorious.core :refer :all]
            [clojure.string :as str])
  (:use midje.sweet)  )

;; Test parse()

(fact "Monglorious parses strings"
      (parse-query "\"foo\"" :string) => ["foo"]
      (parse-query "\"string with \\\" quote\"" :string) => ["string with \" quote"]
      (parse-query "\"string with ' quote\"" :string) => ["string with ' quote"]
      (parse-query "'string with \\' quote'" :string) => ["string with ' quote"]
      (parse-query "'server-status'" :string) => ["server-status"])

(fact "Monglorious parses numbers"
      (parse-query "0" :literal) => [0]
      (parse-query "1" :literal) => [1]
      (parse-query "1.5" :literal) => [1.5]
      (parse-query "-10.5" :literal) => [-10.5])

(fact "Monglorious parses booleans"
      (parse-query "true" :literal) => '(true)
      (parse-query "TRUE" :literal) => '(true)
      (parse-query "false" :literal) => '(false)
      (parse-query "FALSE" :literal) => '(false))

(fact "Monglorious parses lists"
      (parse-query "[1,2,3]" :literal) => [[1,2,3]]
      (parse-query "[ 1, 2, 3 ]" :literal) => [[1,2,3]]
      (parse-query "[true,false,false]" :literal) => [[true,false,false]]
      (parse-query "[[1],[2,3],[4,5,6]]" :literal) => [[[1] [2 3] [4 5 6]]])

(fact "Monglorious parses maps"
      (parse-query "{x:1,y:2,z:3}" :literal) => [{"x" 1 "y" 2 "z" 3}]
      (parse-query "{'x':1,'y':2,'z':3}" :literal) => [{"x" 1 "y" 2 "z" 3}]
      (parse-query "{_id:'123', title:'dragon'}" :literal) => [{"_id" "123" "title" "dragon"}]
      (parse-query "{x:[1,2,3],y:{a:\"b\"}}" :literal) => [{"x" [1 2 3] "y" { "a" "b"}}]
      (parse-query "{x:{y:{z:1}}}" :literal) => [{"x" {"y" {"z" 1}}}])

(fact "Monglorious parses SHOW ___ commands"
      (parse-query "show dbs") => [:show-command [:dbs]]
      (parse-query "show collections") => [:show-command [:collections]]
      (parse-query "show users") => [:show-command [:users]]
      (parse-query "show roles") => [:show-command [:roles]]
      (parse-query "show profile") => [:show-command [:profile]]
      (parse-query "show databases") => [:show-command [:databases]])

(fact "Monglorious parses RunCommand commands"
      (parse-query "db.runCommand(\"foo\")") => [:run-command "foo"]
      (parse-query "db.runCommand('server-status')") => [:run-command "server-status"])

(fact "Monglorious parses DB Collection commands"
      (parse-query "db.fooCollection.count()") => [:collection-command "fooCollection" "count"])
