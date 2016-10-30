(ns monglorious.test-helpers)

(defn check-ok
  [expected response] (= expected (get response "ok")))

(def is-ok? "Checks that a MongoDB response has ok: 1.0"
  (partial check-ok 1.0))

(def is-not-ok? "Checks that a MongoDB response has ok: 0.0"
  (partial check-ok 0.0))
