(ns monglorious.core-test
  (:require [monglorious.core :refer :all]
            [clojure.string :as str])
  (:use midje.sweet)  )

(fact "Monglorious accepts SHOW ___ commands"
      (monglorious-parser "show dbs") => [:command [:show-command [:dbs]]]
      (monglorious-parser "show collections") => [:command [:show-command [:collections]]]
      (monglorious-parser "show users") => [:command [:show-command [:users]]]
      (monglorious-parser "show roles") => [:command [:show-command [:roles]]]
      (monglorious-parser "show profile") => [:command [:show-command [:profile]]]
      (monglorious-parser "show databases") => [:command [:show-command [:databases]]])

(fact "Monglorious accepts DB Collection commands"
      (monglorious-parser "db.fooCollection.count()") => [:command [:collection-command "fooCollection" "count"]])
