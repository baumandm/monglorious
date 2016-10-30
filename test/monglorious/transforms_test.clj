(ns monglorious.transforms-test
  (:require [monglorious.core :refer :all]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:use midje.sweet)
  (:import (com.mongodb MongoQueryException)))

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
             (mc/remove db "documents")
             (mc/insert-batch db "documents" [{:name "Alan" :age 27 :score 17772}
                                              {:name "Joe" :age 32 :score 8277}
                                              {:name "Teresa" :age 31 :score 495044}
                                              {:name "Macy" :age 29 :score 8837777}
                                              {:name "Xavier" :age 1 :score 1234 :child true}
                                              {:name "Zoey" :age 4 :score 100 :child true}
                                              {:name "Roxy" :age 18 :score 104059}
                                              {:name "Anna" :age 18 :score 102459}
                                              {:name "Jack" :age 32 :score 445566}])))
   ;(after :contents
   ;       (let [conn (mg/connect)]
   ;         (mg/drop-db conn "testdb")))]
   ]

  (fact "Monglorious finds documents without any filters"
        (execute {} "testdb" "db.documents.find()") => #(and (coll? %) (= 9 (count %)))
        (execute {} "testdb" "db.documents.FIND()") => #(and (coll? %) (= 9 (count %)))
        (execute {} "testdb" "db.documents.find({})") => #(and (coll? %) (= 9 (count %))))

  (fact "Monglorious finds documents without any filters and projections"
        (execute {} "testdb" "db.documents.find({}, { score: 0 })") => #(and (coll? %)
                                                                             (= 9 (count %))
                                                                             (not-any? (fn [doc] (contains? doc :score)) %))
        (execute {} "testdb" "db.documents.find({}, { name: 1, score: 1 })") => #(and (coll? %)
                                                                                      (= 9 (count %))
                                                                                      (every? (fn [doc] (and (contains? doc :name) (contains? doc :score))) %))
        (execute {} "testdb" "db.documents.find({}, { name: 0, score: 0 })") => #(and (coll? %)
                                                                                      (= 9 (count %))
                                                                                      (not-any? (fn [doc] (or (contains? doc :name) (contains? doc :score))) %))
        (execute {} "testdb" "db.documents.find({}, { name: 0, score: 1 })") => (throws MongoQueryException))

  (fact "Monglorious finds documents with string filters"
        (execute {} "testdb" "db.documents.find({ name: 'Alan' })") => #(and (coll? %) (= 1 (count %)) (= "Alan" (:name (first %))))
        (execute {} "testdb" "db.documents.find({ name: \"Roxy\" })") => #(and (coll? %) (= 1 (count %)) (= "Roxy" (:name (first %))))
        (execute {} "testdb" "db.documents.find({ 'name': 'Joe' })") => #(and (coll? %) (= 1 (count %)) (= "Joe" (:name (first %))))
        (execute {} "testdb" "db.documents.find({ \"name\": \"Xavier\" })") => #(and (coll? %) (= 1 (count %)) (= "Xavier" (:name (first %)))))

  (fact "Monglorious finds documents with boolean filters"
        (execute {} "testdb" "db.documents.find({ child: true })") => #(and (coll? %) (= 2 (count %)))
        (execute {} "testdb" "db.documents.find({ child: false })") => #(and (coll? %) (= 0 (count %))))

  (fact "Monglorious finds documents with numerical filters"
        (execute {} "testdb" "db.documents.find({ age: 18 })") => #(and (coll? %) (= 2 (count %)))
        (execute {} "testdb" "db.documents.find({ score: 100, child: true })") => #(and (coll? %) (= 1 (count %)) (= "Zoey" (:name (first %)))))

  (fact "Monglorious finds one document without any filters"
        (execute {} "testdb" "db.documents.findOne()") => #(and (map? %) (contains? % :name))
        (execute {} "testdb" "db.documents.FINDONE()") => #(and (map? %) (contains? % :name))
        (execute {} "testdb" "db.documents.findOne({})") => #(and (map? %) (contains? % :name)))

  (fact "Monglorious finds one document with filters"
        (execute {} "testdb" "db.documents.findOne({ name: 'Alan' })") => #(and (map? %) (= "Alan" (:name %)))
        (execute {} "testdb" "db.documents.findOne({ age: 1 })") => #(and (map? %) (= "Xavier" (:name %)))
        (execute {} "testdb" "db.documents.findOne({ child: true })") => #(and (map? %) (= "Xavier" (:name %))))

  (fact "Monglorious finds one document with projections"
        (execute {} "testdb" "db.documents.findOne({}, { score: 0 })") => #(and (map %) (not (contains? % :score)))
        (execute {} "testdb" "db.documents.findOne({}, { name: 1, score: 1 })") => #(and (map %) (contains? % :score) (contains? % :name))
        (execute {} "testdb" "db.documents.findOne({}, { name: 0, score: 0 })") => #(and (map %) (not (contains? % :name)) (not (contains? % :score)))
        (execute {} "testdb" "db.documents.findOne({}, { name: 0, score: 1 })") => (throws MongoQueryException))

  (fact "Monglorious counts documents without any filters"
        (execute {} "testdb" "db.documents.count()") => 9
        (execute {} "testdb" "db.documents.COUNT()") => 9
        (execute {} "testdb" "db.documents.count({})") => 9)

  (fact "Monglorious counts documents with filters"
        (execute {} "testdb" "db.documents.count({ child: true })") => 2
        (execute {} "testdb" "db.documents.COUNT({ name: 'Roxanne' })") => 0
        (execute {} "testdb" "db.documents.count({ 'age': 32})") => 2))
