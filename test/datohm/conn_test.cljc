(ns datohm.conn-test
  (:require [datohm.conn :as conn]
            [datohm.test-utils :as utils]
            [#?(:clj  clojure.spec
                :cljs cljs.spec)
             :as s]
            [#?(:clj  clojure.spec.gen
                :cljs cljs.spec.impl.gen)
             :as gen]
            [#?(:clj  clojure.spec.test
                :cljs cljs.spec.test)
             :include-macros true
             :as stest]
            [#?(:clj  clojure.test
                :cljs cljs.test)
             :as t
             :include-macros true
             #?(:clj  :refer
                :cljs :refer-macros)
             [deftest testing is]]
            [clojure.test.check :as stc]
            [clojure.test.check.properties]))

(stest/instrument)

#?(:clj
   (do
     (deftest fn-specs
       (utils/test-checkable-syms (utils/specs "datohm.conn")))

     (deftest datomic-uri-test
       (is (= "datomic:dev://localhost:4334/test"
              (conn/datomic-uri "test")))
       (is (= "datomic:ddb://us-east-1/test/test"
              (conn/datomic-uri "ddb" "us-east-1/test" "test")))
       (is (= "datomic:ddb://us-east-1/test/test?aws_access_key_id=XXX&aws_secret_key=XXX"
              (conn/datomic-uri "ddb" "us-east-1/test" "test" "XXX" "XXX"))))))

(deftest connect-test
  (testing "idempotent"
    (is (= (conn/connect "datohm.conn-test")
           (conn/connect "datohm.conn-test")))))
