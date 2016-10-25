(ns monglorious.transforms
  (:require [monger.core :as mg]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]))

(defn run-command-transform
  [command]
  (cond
    (= command "serverStatus")
    (fn [_ db] (from-db-object (mg-cmd/server-status db) false))

    (= command "dbStats")
    (fn [_ db] (from-db-object (mg-cmd/db-stats db) false))

    (and (map? command) (contains? command "collStats"))
    (fn [_ db] (from-db-object (mg-cmd/collection-stats db (get command "collStats")) false))

    :else
    (throw (Exception. "Unsupported database command."))))

