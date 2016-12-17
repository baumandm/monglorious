(ns monglorious.core
  (:require [monglorious.parser :as parser]
            [monger.core :as mg])
  (:gen-class
    :name org.baumandm.monglorious.Monglorious
    :main false
    :methods [#^{:static true} [execute [String String] Object]
              #^{:static true} [executeWithConnection [com.mongodb.MongoClient com.mongodb.DB String] Object]
              #^{:static true} [getConnection [String] Object]]))

(defn get-connection
  "Returns a connection and db object that can be reused multiple times"
  ([connection-uri]
   (if (string? connection-uri)
     (mg/connect-via-uri connection-uri)
     (throw (Exception. "Missing db-name argument when not using URI."))))
  ([connection-options-or-uri db-name]
   (if (string? connection-options-or-uri)
     (mg/connect-via-uri connection-options-or-uri)
     (let [conn (mg/connect connection-options-or-uri)
           db (mg/get-db conn db-name)]
       {:conn conn :db db}))))

(defn execute-with-connection
  "Executes a MongoDB query given an open connection and db"
  [{:keys [conn db]} query]
  (let [compiled-query (parser/compile-query query)
        db (if (string? db) (mg/get-db conn db) db)]
    ;; Execute query using connection and db
    (compiled-query conn db)))

(defn execute
  "Connects and executes a MongoDB query, using a connection map or URI"
  ([connection-uri query]
   (let [connection (get-connection connection-uri)
         result (execute-with-connection connection query)]
     (mg/disconnect (:conn connection))
     result))
  ([connection-options-or-uri db-name query]
   (let [connection (get-connection connection-options-or-uri db-name)
         result (execute-with-connection connection query)]
     (mg/disconnect (:conn connection))
     result))
  )

;; Java Interop implementation

(defrecord MongloriousConnection
  [conn db])

(defn -execute
  "A Java-callable wrapper around 'execute'."
  [uri query]
  (execute uri query))

(defn -executeWithConnection
  "A Java-callable wrapper around 'execute-with-connection'."
  [connection db query]
  (execute-with-connection {:conn connection :db db} query))

(defn -getConnection
  "A Java-callable wrapper around 'get-connection'."
  [uri]
  (map->MongloriousConnection (get-connection uri)))
