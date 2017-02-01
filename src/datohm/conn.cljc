(ns datohm.conn
  (:require [#?(:clj  clojure.spec
                :cljs cljs.spec)
             :as s]
            [#?(:clj  clojure.spec.test
                :cljs cljs.spec.test)
             :include-macros true
             :as stest]
            [#?(:clj  clojure.spec.gen
                :cljs cljs.spec.impl.gen)
             :as gen]
            [#?(:clj datomic.api
                :cljs datascript.core) :as d]
            #?(:clj [environ.core :refer [env]])
            [taoensso.timbre :as log]))

#?(:clj
   (do
     (defn datomic-uri
       ([name] (datomic-uri "dev" "localhost:4334" name))
       ([storage uri name] (datomic-uri storage uri name nil nil))
       ([storage uri name aws-access-key-id aws-secret-key]
        (cond-> (format "datomic:%s://%s/%s"
                        storage uri name)
          (and aws-access-key-id
               aws-secret-key) (str (format "?aws_access_key_id=%s&aws_secret_key=%s"
                                            aws-access-key-id aws-secret-key)))))

     (defn uri-from-env
       [name]
       (datomic-uri (env :datomic-storage "dev")
                    (env :datomic-uri "localhost:4334")
                    name
                    (env :aws-access-key-id)
                    (env :aws-secret-key)))

     (defn unique-db
       []
       (uri-from-env (d/squuid)))

     (defn connect
       [db-name]
       (let [db-uri (uri-from-env db-name)]
         (d/create-database db-uri)
         (d/connect db-uri))))
   :cljs
   (do
     (enable-console-print!)))
