(ns monglorious.core
  (:gen-class)
  (:require [monglorious.parser :as parser]
            [monger.core :as mg]))

(defn get-connection
  "Returns a connection and db object that can be reused multiple times"
  [connection-options-or-uri db-name]
  (if (string? connection-options-or-uri)
    (mg/connect-via-uri connection-options-or-uri)
    (let [conn (mg/connect connection-options-or-uri)
          db (mg/get-db conn db-name)]
      {:conn conn :db db})))

(defn execute-with-connection
  "Executes a MongoDB query given an open connection and db"
  [{:keys [conn db]} query]
  (let [compiled-query (parser/compile-query query)
        db (if (string? db) (mg/get-db conn db) db)]
    ;; Execute query using connection and db
    (compiled-query conn db)))

(defn execute
  "Connects and executes a MongoDB query, using a connection map or URI"
  [connection-options-or-uri db-name query]
  (let [connection (get-connection connection-options-or-uri db-name)
        result (execute-with-connection connection query)]
    (mg/disconnect (:conn connection))
    result))
