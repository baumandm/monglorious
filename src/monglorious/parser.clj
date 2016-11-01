(ns monglorious.parser
  (:require [monglorious.transforms :refer :all]
            [instaparse.core :as insta]
            [instaparse.transform :as insta-transform]
            [monger.core :as mg]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]
            [clojure.string :refer [lower-case]])
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
     :db-object            (fn [db-object] (first db-object))
     :function-application (fn [name & args] (into [(lower-case name)] args))
     }
    tree))

(defn parse-unsimplified
  "Parse a MongoDB query into an unsimplified expression tree."
  ([query] (parse-unsimplified query :query))
  ([query start]
   (let [tree (monglorious-parser query :start start)]
     (if (insta/failure? tree)
       (throw (Exception. ^String (with-out-str (print tree))))
       tree))))

(defn parse-query
  "Parses a MongoDB query and simplifies"
  ([query] (parse-query query :query))
  ([query start] (-> query
                     (parse-unsimplified start)
                     (simplfy))))

(defn compile-query
  "Parses and compiles a MongoDB query into a function accepting (conn db)"
  [query]
  (->> query
       (parse-query)
       (insta-transform/transform
         {:run-command        run-command-transform
          :show-command       show-command-transform
          :collection-command collection-command-transform
          })))
