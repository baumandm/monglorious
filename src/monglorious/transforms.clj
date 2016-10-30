(ns monglorious.transforms
  (:require [monger.core :as mg]
            [monger.collection :as mg-coll]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]
            [monger.db :as mg-db]
            [clojure.string :refer [lower-case]]))


(defn run-command-transform
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
  [db-object]
  (case db-object
    :dbs
    (fn [conn _] (into [] (mg/get-db-names conn)))
    :databases
    (fn [conn _] (into [] (mg/get-db-names conn)))
    :collections
    (fn [conn db] (into [] (mg-db/get-collection-names db)))

    (throw (Exception. "Unsupported database object."))))

(defn collection-command-transform
  "Handle"
  [collection-name & function-applications] ;;function-name & args]
  (case (count function-applications)
    ;; One function
    1 (let [function-application (first function-applications)
            function-name (lower-case (first function-application))
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

        (throw (Exception. (format "Unsupported function: %s." function-name)))))))
