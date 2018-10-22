(ns monglorious.transforms
  (:require [monger.core :as mg]
            [monger.collection :as mg-coll]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]
            [monger.db :as mg-db]
            [monger.query :as mg-q]
            [clojure.string :refer [lower-case]])
  (:import (com.mongodb DB MongoQueryException)))

;; Transformers that convert the parsed query tree into applicable functions.
;; Each transformer returns a function that takes (conn db),
;; executes the specified query, and returns the result.

(defn run-command-transform
  "Transform a run command into a function that takes (conn db) and returns a result."
  [command]
  (let [command (if (string? command) (clojure.string/lower-case command) command)]
    (cond
      (= command "serverstatus")
      (fn [_ db] (from-db-object (mg-cmd/server-status db) false))

      (= command "dbstats")
      (fn [_ db] (from-db-object (mg-cmd/db-stats db) false))

      (= command "whatsmyuri")
      (fn [_ db] (from-db-object (mg/command db {:whatsmyuri 1}) false))

      ;; Anything passed as a map is passed directly into mg/command
      (map? command)
      (fn [_ db] (from-db-object (mg/command db command) false))

      :else
      (throw (Exception. "Unsupported database command.")))))

(defn show-command-transform
  "Transform a show __ command into a function that takes (conn db) and returns a result."
  [db-object]
  (case db-object
    :dbs
    (fn [conn _] (vec (mg/get-db-names conn)))
    :databases
    (fn [conn _] (vec (mg/get-db-names conn)))
    :collections
    (fn [_ db] (vec (mg-db/get-collection-names db)))

    (throw (Exception. "Unsupported database object."))))

(defn collection-command-transform
  "Transform a db collection command into a function that takes (conn db) and returns a result."
  [collection-name & function-applications]
  ;; Handle based on number of functions chained together
  ;; Need to consider the entire chain, vs executing each one in order
  (if (= 1 (count function-applications))
    ;; One function
    (let [function-application (first function-applications)
          function-name (first function-application)
          args (rest function-application)]
      (case function-name
        "find"
        (fn [_ db] (doall (apply (partial mg-coll/find-maps db collection-name) args)))

        "findone"
        (let [args (if (empty? args) [{}] args)]
          (fn [_ db] (doall (apply (partial mg-coll/find-one-as-map db collection-name) args))))

        "count"
        (let [conditions (if (empty? args) {} (first args))]
          (fn [_ db] (mg-coll/count db collection-name conditions)))

        "stats"
        (fn [_ db] (mg-cmd/collection-stats db collection-name))

        "datasize"
        (fn [_ db] (get (mg-cmd/collection-stats db collection-name) "size"))

        "storagesize"
        (fn [_ db] (get (mg-cmd/collection-stats db collection-name) "storageSize"))

        "totalindexsize"
        (fn [_ db] (get (mg-cmd/collection-stats db collection-name) "totalIndexSize"))

        "getindexes"
        (fn [_ db] (mg-coll/indexes-on db collection-name))

        "aggregate"
        (fn [_ db] (let [[stages opts] args
                         opts' (->> (-> opts
                                        (or {"cursor" {}})
                                        (clojure.walk/keywordize-keys))
                                    (into [])
                                    (apply concat))]
                     (apply (partial mg-coll/aggregate db collection-name stages) opts')))

        "insert"
        (let [document-or-documents (first args)]
          (if (sequential? document-or-documents)
            (fn [_ db] (mg-coll/insert-batch db collection-name document-or-documents))
            (fn [_ db] (apply (partial mg-coll/insert-and-return db collection-name) args))
            ))

        "insertone"
        (fn [_ db] (apply (partial mg-coll/insert-and-return db collection-name) args))

        "drop"
        (fn [_ db] (apply (partial mg-coll/drop db collection-name) args))

        (throw (Exception. (format "Unsupported function: %s." function-name)))))

    ;; More than one function
    (let [function-names (map first function-applications)
          function-args (map rest function-applications)]
      (cond

        ;; Special case .find().count()
        (= function-names ["find" "count"])
        (collection-command-transform collection-name (into ["count"] (first function-args)))

        :else
        (fn [_ ^DB db]
          (let [query (loop [fns function-applications
                           query (mg-q/empty-query (.getCollection db collection-name))]
                      (let [fn (first fns)
                            function-name (first fn)
                            first-arg (first (rest fn))
                            query (case function-name
                                    "find" (if (= 3 (count fn))
                                                  (-> query
                                                      (mg-q/find (or first-arg {}))
                                                      (mg-q/fields (nth fn 2)))
                                                  (mg-q/find query (or first-arg {})))
                                    "sort" (mg-q/sort query first-arg)
                                    "skip" (mg-q/skip query first-arg)
                                    "limit" (mg-q/limit query first-arg)
                                    "batchsize" (mg-q/batch-size query first-arg)
                                    ;; else
                                    (throw (MongoQueryException. (.getAddress (.getMongo db)) -1 (format "%s() is not a function" function-name))))]
                        (if (= 1 (count fns))
                          query
                          (recur (rest fns) query))))]
          (doall (mg-q/exec query))))))))
