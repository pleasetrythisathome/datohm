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

;; ========== Preds ==========

(def db?
  (partial instance? #?(:clj  datomic.db.Db
                        :cljs datascript.db.DB)))

(def conn?
  (partial instance? #?(:clj  datomic.Connection
                        :cljs cljs.core.Atom)))

;; ========== Conn ==========

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
       ([] (connect (env :datomic-db-name "datohm")))
       ([db-name]
        (let [db-uri (uri-from-env db-name)]
          (d/create-database db-uri)
          (d/connect db-uri))))

     (s/fdef connect
             :args (s/? (s/and string? seq))
             :ret conn?))
   :cljs
   (do
     (enable-console-print!)))

;; ========== Protocols ==========

(defprotocol DatomicConnection
  (as-conn [_]))

(defprotocol DatabaseReference
  (as-db [_]))

(extend-protocol DatomicConnection
  #?(:clj  datomic.Connection
     :cljs cljs.core.Atom)
  (as-conn [c]
    c)
   #?(:clj  clojure.lang.IPersistentMap
     :cljs cljs.core.PersistentArrayMap)
  (as-conn [env]
    (let [{:keys [conn]} env]
      (assert conn "env missing :conn")
      conn)))

(s/fdef as-conn
        :args (s/cat :conn :datohm/conn)
        :ret conn?)

(extend-protocol DatabaseReference
  #?(:clj  datomic.db.Db
     :cljs datascript.db.DB)
  (as-db [db]
    db)
  #?(:clj  datomic.Connection
     :cljs cljs.core.Atom)
  (as-db [db]
    (d/db db))
  #?(:clj  clojure.lang.IPersistentMap
     :cljs cljs.core.PersistentArrayMap)
  (as-db [{:keys [tx/mode tx/result] :as env}]
    (or (and (= :tx.mode/with mode)
             (:db-after result))
        (as-db (as-conn env)))))

(s/fdef as-db
        :args (s/cat :db :datohm/db)
        :ret db?)

;; ========== Specs ==========

(s/def :datomic/conn
  (s/with-gen conn?
    #(gen/return (connect))))

(s/def :datomic/db
  (s/with-gen db?
    #(gen/return (d/db (connect)))))

(s/def :datohm/conn
  (s/or :conn :datomic/conn
        :env (s/keys :req-un [:datomic/conn])))

(s/def :datohm/db
  (s/or :conn :datomic/conn
        :db :datomic/db
        :env (s/keys :req-un [:datomic/conn])))
