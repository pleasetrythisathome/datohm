(ns datohm.conn-test
  (:require [datohm.config :as config]
            [datohm.conn :as conn]
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

(conn/connect! (-> "datohm"
                   (config/config :test)
                   (get-in [:datomic :location])
                   (conn/datomic-uri)))

#?(:clj
   (do
     (deftest fn-specs
       (utils/test-checkable-syms (utils/specs "datohm.conn")))

     (deftest datomic-uri-test
       (is (= "datomic:dev://localhost:4334/test"
              (conn/datomic-uri {:storage "dev"
                                 :db-name "test"})))
       (is (= "datomic:dev://0.0.0.0:5000/test"
              (conn/datomic-uri {:storage "dev"
                                 :host "0.0.0.0"
                                 :port 5000
                                 :db-name "test"})))
       (is (= "datomic:ddb://us-east-1/table/test"
              (conn/datomic-uri {:storage "ddb"
                                 :region "us-east-1"
                                 :table "table"
                                 :db-name "test"})))
       (is (= "datomic:ddb://us-east-1/table/test?aws_access_key_id=XXX&aws_secret_key=XXX"
              (conn/datomic-uri {:storage "ddb"
                                 :region "us-east-1"
                                 :table "table"
                                 :db-name "test"
                                 :aws-access-key-id "XXX"
                                 :aws-secret-key "XXX"})))
       (is (= "datomic:ddb-local://0.0.0.0:5000/table/test"
              (conn/datomic-uri {:storage "ddb-local"
                                 :host "0.0.0.0"
                                 :port 5000
                                 :table "table"
                                 :db-name "test"})))
       (is (= "datomic:ddb-local://0.0.0.0:5000/table/test?aws_access_key_id=XXX&aws_secret_key=XXX"
              (conn/datomic-uri {:storage "ddb-local"
                                 :host "0.0.0.0"
                                 :port 5000
                                 :table "table"
                                 :db-name "test"
                                 :aws-access-key-id "XXX"
                                 :aws-secret-key "XXX"}))))))
