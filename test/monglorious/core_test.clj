(ns monglorious.core-test
  (:require [monglorious.core :refer :all]
            [monglorious.test-helpers :refer :all])
  (:use midje.sweet))

;; Test execute()

(fact "Monglorious connects without configuration (to localhost) and executes commands"
      (execute {} "testing" "db.runCommand('serverStatus')") => is-ok?
      (execute {} "testing" "db.runCommand('dbStats')") => is-ok?)

(fact "Monglorious connects given configuration, and executes commands"
      (execute {:host "localhost" :port 27017} "testing" "db.runCommand('serverStatus')") => is-ok?
      (execute {:host "localhost" :port 27017} "testing" "db.runCommand('dbStats')") => is-ok?)

(fact "Monglorious connects via uri and executes commands"
      (execute "mongodb://127.0.0.1/testing" "db.runCommand('serverStatus')") => is-ok?
      (execute "mongodb://127.0.0.1/testing" "db.runCommand('dbStats')") => is-ok?)

(fact "Monglorious connects via uri and db and executes commands"
      (execute "mongodb://127.0.0.1/testing" "testing" "db.runCommand('serverStatus')") => is-ok?
      (execute "mongodb://127.0.0.1/testing" "testing" "db.runCommand('dbStats')") => is-ok?)

(let [conn (get-connection "mongodb://127.0.0.1/testing")]
  (fact "Monglorious connects via uri and executes commands"
        (execute-with-connection conn "db.runCommand('serverStatus')") => is-ok?
        (execute-with-connection conn "db.runCommand('dbStats')") => is-ok?))
