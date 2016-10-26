(ns monglorious.transforms-test
  (:require [monglorious.core :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:use midje.sweet))

;; Test run-command-transform()

(defn check-ok
  [expected response] (= expected (get response "ok")))

(def is-ok? "Checks that a MongoDB response has ok: 1.0"
  (partial check-ok 1.0))

(def is-not-ok? "Checks that a MongoDB response has ok: 0.0"
  (partial check-ok 0.0))

;; Create some sample data before tests, delete after
(against-background
  [(before :contents
           (let [conn (mg/connect)
                 db (mg/get-db conn "testdb")]
             (mc/insert-batch db "documents" [{:name "Alan" :age 27 :score 17772}
                                              {:name "Joe" :age 32 :score 8277}
                                              {:name "Macy" :age 29 :score 8837777}])))
   (after :contents
          (let [conn (mg/connect)]
            (mg/drop-db conn "testdb")))]

  ;; Test run-command-transform()
  (fact "Monglorious executes serverStatus"
        (execute {} "testdb" "db.runCommand('serverStatus')") => is-ok?
        (execute {} "testdb" "db.runCommand('serverstatus')") => is-ok?
        (execute {} "testdb" "db.runCommand(\"SERVERSTATUS\")") => is-ok?
        (execute {} "testdb" "db.runCommand({serverStatus: 1})") => is-ok?)

  (fact "Monglorious executes dbStats"
        (execute {} "testdb" "db.runCommand('dbStats')") => is-ok?
        (execute {} "testdb" "db.runCommand('dbstats')") => is-ok?
        (execute {} "testdb" "db.runCommand({dbStats: 1})") => is-ok?)

  (fact "Monglorious executes collStats"
        (execute {} "testdb" "db.runCommand({collStats: 'unknownCollection'})") => is-not-ok?
        (execute {} "testdb" "db.runCommand({collStats: 'documents'})") => is-ok?
        (execute {} "testdb" "db.runCommand({'collStats': 'documents'})") => is-ok?
        (execute {} "testdb" "db.runCommand({\"collStats\": \"documents\"})") => is-ok?)

  (fact "Monglorious executes whatsmyuri"
        (execute {} "testdb" "db.runCommand('whatsmyuri')") => (just {"ok" 1.0 "you" string?})
        (execute {} "testdb" "db.runCommand({whatsmyuri: 1})") => (just {"ok" 1.0 "you" string?})))

; Create some sample data before tests, delete after
(against-background
  [(before :contents
           (let [conn (mg/connect)
                 db (mg/get-db conn "testdb")
                 db2 (mg/get-db conn "testdb2")]
             (mc/insert-batch db "documents" [{:name "Alan" :age 27 :score 17772}
                                              {:name "Joe" :age 32 :score 8277}
                                              {:name "Macy" :age 29 :score 8837777}])
             (mc/insert-batch db2 "secret-documents" [{:key 123}
                                                      {:key 345}
                                                      {:key 567}])))
   (after :contents
          (let [conn (mg/connect)]
            (mg/drop-db conn "testdb")
            (mg/drop-db conn "testdb2")))]

  ;; Test show-command-transform()
  (fact "Monglorious executes show dbs"
        (execute {} "testdb" "show dbs") => (contains #{"testdb" "testdb2"} :gaps-ok)
        (execute {} "testdb" "SHOW DBS") => (contains #{"testdb" "testdb2"} :gaps-ok))

  (fact "Monglorious executes show databases"
        (execute {} "testdb" "show databases") => (contains #{"testdb" "testdb2"} :gaps-ok)
        (execute {} "testdb" "SHOW DATABASES") => (contains #{"testdb" "testdb2"} :gaps-ok))

  (fact "Monglorious executes show collections"
        (execute {} "testdb" "show collections") => (contains #{"documents"})
        (execute {} "testdb2" "SHOW COLLECTIONS") => (contains #{"secret-documents"}))
  )
