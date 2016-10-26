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


;; Test run-command-transform()
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


;; Test show-command-transform()
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


;; Test collection-command-transform()
(against-background
  [(before :contents
           (let [conn (mg/connect)
                 db (mg/get-db conn "testdb")]
             (mc/insert-batch db "documents" [{:name "Alan" :age 27 :score 17772}
                                              {:name "Joe" :age 32 :score 8277}
                                              {:name "Teresa" :age 31 :score 495044}
                                              {:name "Macy" :age 29 :score 8837777}
                                              {:name "Xavier" :age 1 :score 1234 :child true}
                                              {:name "Zoey" :age 4 :score 100 :child true}
                                              {:name "Roxy" :age 18 :score 104059}])))
   (after :contents
          (let [conn (mg/connect)]
            (mg/drop-db conn "testdb")))]

  (fact "Monglorious finds documents without any filters"
        (execute {} "testdb" "db.documents.find()") => #(and (coll? %) (= 7 (count %)))
        (execute {} "testdb" "db.documents.find({})") => #(and (coll? %) (= 7 (count %)))
        (execute {} "testdb" "db.documents.FIND()") => #(and (coll? %) (= 7 (count %))))

  (fact "Monglorious finds documents with string filters"
        (execute {} "testdb" "db.documents.find({ name: 'Alan' })") => #(and (coll? %) (= 1 (count %)) (= "Alan" (:name (first %))))
        (execute {} "testdb" "db.documents.find({ name: \"Roxy\" })") => #(and (coll? %) (= 1 (count %)) (= "Roxy" (:name (first %)))))

  (fact "Monglorious finds documents with boolean filters"
        (execute {} "testdb" "db.documents.find({ child: true })") => #(and (coll? %) (= 2 (count %))))

  (fact "Monglorious finds one document without any filters"
        (execute {} "testdb" "db.documents.findOne()") => #(and (map? %) (contains? % :name))
        (execute {} "testdb" "db.documents.findOne({})") => #(and (map? %) (contains? % :name))
        (execute {} "testdb" "db.documents.FINDONE()") => #(and (map? %) (contains? % :name)))
  )
