(ns monglorious.parser-test
  (:require [monglorious.parser :refer :all]
            [clj-time.format :as f]
            [clj-time.coerce :as c])
  (:use midje.sweet)
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;; Test parse-query()

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

(fact "Monglorious parses Regexes"
      (parse-query "/^a.*/" :literal) => [{"$regex" "^a.*"}]
      (parse-query "/^a.*/i" :literal) => [{"$regex" "^a.*" "$options" "i"}])

(fact "Monglorious parses ObjectIds"
      (parse-query "ObjectId('581d36e347aee26883837eb7')" :literal) => [(monger.util/object-id "581d36e347aee26883837eb7")]
      (parse-query "ObjectId(\"581d36e347aee26883837eb7\")" :literal) => [(monger.util/object-id "581d36e347aee26883837eb7")])

(fact "Monglorious parses Dates"
      (parse-query "Date()" :literal) => (fn [[date]]
                                           (and
                                             (string? date)
                                             (= (.format (SimpleDateFormat. "EEE MMM dd yyyy HH:mm:ss 'GMT'Z '('zzz')'") (Date.)) date)))
      (parse-query "new Date()" :literal) => (fn [[date]] (instance? Date date))
      (parse-query "ISODate()" :literal) => (fn [[date]] (instance? Date date))
      (parse-query "new ISODate()" :literal) => (fn [[date]] (instance? Date date))
      (parse-query "ISODate('2017-02-25T15:23:59.340Z')" :literal) => (fn [[date]] (= date (c/to-date (f/parse "2017-02-25T15:23:59.340Z")))))

(fact "Monglorious parses SHOW ___ commands"
      (parse-query "show dbs") => [:show-command :dbs]
      (parse-query "show collections") => [:show-command :collections]
      (parse-query "show users") => [:show-command :users]
      (parse-query "show roles") => [:show-command :roles]
      (parse-query "show profile") => [:show-command :profile]
      (parse-query "show databases") => [:show-command :databases])

(fact "Monglorious parses RunCommand commands"
      (parse-query "db.runCommand(\"foo\")") => [:run-command "foo"]
      (parse-query "db.runCommand('serverStatus')") => [:run-command "serverStatus"])

(fact "Monglorious parses DB Collection commands"
      (parse-query "db.fooCollection.count()") => [:collection-command "fooCollection" ["count"]]
      (parse-query "db.fooCollection.count({})") => [:collection-command "fooCollection" ["count" {}]]
      (parse-query "db.fooCollection.find()") => [:collection-command "fooCollection" ["find"]]
      (parse-query "db.fooCollection.find({name: 'xx'})") => [:collection-command "fooCollection" ["find" {"name" "xx"}]]
      (parse-query "db.fooCollection.find({name: 'xx'}, {yy: false})") => [:collection-command "fooCollection" ["find" {"name" "xx"} {"yy" false}]]
      (parse-query "db.fooCollection.find({name: { $eq: 'xx' }})") => [:collection-command "fooCollection" ["find" {"name" { "$eq" "xx"}}]])
