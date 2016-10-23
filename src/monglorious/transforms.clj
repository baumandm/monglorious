(ns monglorious.transforms
  (:require [monger.core :as mg]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]))

(defn run-command-transform
  [command-name]
  (case command-name
    "serverStatus"
    (fn [_ db] (from-db-object (mg-cmd/server-status db) false))
    "dbStats"
    (fn [_ db] (from-db-object (mg-cmd/db-stats db) false))

    (throw (Exception. "Unsupported database command."))))
