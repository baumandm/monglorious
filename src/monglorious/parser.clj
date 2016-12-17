(ns monglorious.parser
  (:require [monglorious.transforms :refer :all]
            [instaparse.core :as insta]
            [instaparse.transform :as insta-transform]
            [monger.core :as mg]
            [monger.command :as mg-cmd]
            [monger.conversion :refer [from-db-object]]
            [clojure.string :refer [lower-case]]
            [clojure.walk :refer [postwalk]])
  (:import (org.apache.commons.lang3 StringEscapeUtils)))

(defn- flatten-map
  [form]
  (into {} (mapcat (fn [[k v]]
                     (if (map? v)
                       (flatten-map v)
                       [[k v]]))
                   form)))

(def ^{:private true} whitespace
  (insta/parser
    "whitespace = #'\\s+'"))

(def ^{:private true} monglorious-parser
  (time (insta/parser (clojure.java.io/resource "monglorious-grammar.ebnf")
                      :string-ci true
                      :auto-whitespace whitespace)))

(defn- trim-string
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
  (->> tree

       ;; Replace various parser elements
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
          :objectid             (fn [value] (if (nil? value)
                                              (monger.util/object-id)
                                              (monger.util/object-id value)))
          :db-object            (fn [db-object] (first db-object))
          :function-application (fn [name & args] (into [(lower-case name)] args))
          :regex                (fn [& args] (zipmap ["$regex" "$options"] args))
          })

       ;; Post-walk optimizations
       (postwalk (fn [form]
                                (cond
                                  (and (map? form)
                                       (contains? form "$regex")
                                       (map? (get form "$regex")))
                                  (flatten-map form)
                                  :else form)))))

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
