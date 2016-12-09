# Monglorious!

Monglorious is a MongoDB client library which 
accepts textual queries in the syntax of the MongoDB shell. 

Monglorious is written in Clojure.

[![Build Status](https://travis-ci.org/baumandm/monglorious.svg?branch=master)](https://travis-ci.org/baumandm/monglorious) [![Dependencies Status](https://jarkeeper.com/baumandm/monglorious/status.svg)](https://jarkeeper.com/baumandm/monglorious)

## Examples

    (execute "mongodb://localhost:27017/testdb" "db.documents.count()")
    => 9
     
    (execute "mongodb://localhost:27017/testdb" "db.documents.find({ name: 'Alan' })")
    => ({:_id #object[org.bson.types.ObjectId 0x7acd5871 "5815c9d9b160550f0eab8868"], :name "Alan", :age 27, :score 17772})

## Installation

Add the following dependency to your project.clj file:

[![](https://clojars.org/monglorious/latest-version.svg)](https://clojars.org/monglorious)

## Under Development

This library is in fledgeling development mode.  Stay tuned for updates.

Java bindings are on the roadmap.

### Running Tests

Run all the tests with [Midje](https://github.com/marick/Midje/wiki/Running-midje):

    lein midje
    
Add :autotest to watch for changes and re-run tests as the source changes.

    lein midje :autotest

## Building API Documentation

[Codox](https://github.com/weavejester/codox) is used to generate API documentation for Monglorious.

    lein codox

This generates files in `target/base+system+user+dev/doc`.  Copy these files over to the `gh-pages` branch.

## License

Copyright © 2016 Dave Bauman

Distributed under the Eclipse Public License, the same as Clojure.
