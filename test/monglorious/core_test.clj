(ns monglorious.core-test
  (:require [monglorious.core :refer :all])
  (:use midje.sweet))

;; Test execute()

(fact "Monglorious connects without configuration (to localhost) and executes commands"
      (execute {} "testing" "db.runCommand('serverStatus')") => map?
      (execute {} "testing" "db.runCommand('dbStats')") => map?)

(fact "Monglorious connects given configuration, and executes commands"
      (execute {:host "localhost" :port 27017} "testing" "db.runCommand('serverStatus')") => map?
      (execute {:host "localhost" :port 27017} "testing" "db.runCommand('dbStats')") => map?)

(fact "Monglorious connects via uri and executes commands"
      (execute "mongodb://127.0.0.1/testing" "testing" "db.runCommand('serverStatus')") => map?
      (execute "mongodb://127.0.0.1/testing" "testing" "db.runCommand('dbStats')") => map?)

(let [conn (get-connection "mongodb://127.0.0.1/testing")]
  (fact "Monglorious connects via uri and executes commands"
        (execute-with-connection conn "db.runCommand('serverStatus')") => map?
        (execute-with-connection conn "db.runCommand('dbStats')") => map?))
