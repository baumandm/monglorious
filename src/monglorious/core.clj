(ns monglorious.core
  (:gen-class)
  (:require [instaparse.core :as insta]
            [instaparse.transform :as insta-transform]
            [monger.core :as mg]
            [monger.command :as mg-cmd])
  (:import (org.apache.commons.lang3 StringEscapeUtils)))

(def ^{:private true} whitespace
  (insta/parser
    "whitespace = #'\\s+'"))

(def monglorious-parser
  (time (insta/parser (clojure.java.io/resource "grammar.ebnf")
                      :string-ci true
                      :auto-whitespace whitespace)))

(defn trim-string
  [quote-char string]
  (letfn [(trim [s] (subs s (count quote-char) (- (count s) (count quote-char))))
          (quotes [s] (clojure.string/replace s (str "\\" quote-char) quote-char))
          (unescape [s] (StringEscapeUtils/unescapeJava s))]
    (-> string
        trim
        quotes
        unescape)))

(defn simplfy
  "Simplifies an expression tree"
  [tree]
  (insta-transform/transform
    {:query                identity
     :TRUE                 #(do true)
     :FALSE                #(do false)
     :boolean              identity
     :identifier           identity
     :single-quoted-string (partial trim-string "'")
     :double-quoted-string (partial trim-string "\"")
     :string               identity
     :number               (fn [s] (let [n (clojure.edn/read-string s)] (when n n)))
     :list                 (fn [& args] (into [] args))
     :map-item             (fn [key value] [(name key) value])
     :map                  (fn [& args]
                             (let [keys (take-nth 2 args)
                                   vals (take-nth 2 (rest args))]
                               (zipmap keys vals)))
     }
    tree))

(defn parse-unsimplified
  "Parse a MongoDB query into an unsimplified expression tree."
  [query start]
  (let [tree (monglorious-parser query :start start)]
    (if (insta/failure? tree)
      (throw (Exception. ^String (with-out-str (print tree))))
      tree)))

(defn parse-query
  "Parses a MongoDB query and simplifies"
  ([query] (parse-query query :query))
  ([query start] (-> query
                     (parse-unsimplified start)
                     (simplfy))))

(defn compile-query
  "Parses and compiles a MongoDB query"
  [query]
  (->> query
       (parse-query)
       (insta-transform/transform
         {:run-command
          (fn [command-name]
            (case command-name
              "serverStatus"
              (fn [conn db] (mg-cmd/server-status db))
              "dbStats"
              (fn [conn db] (mg-cmd/db-stats db))

              (throw (Exception. "Unsupported database command."))))
          })))

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
  (let [compiled-query (compile-query query)
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
