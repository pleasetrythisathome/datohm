(ns datohm.conn
  (:require [datohm.ctr :as ctr]
            [#?(:clj  clojure.spec
                :cljs cljs.spec)
             :as s]
            [#?(:clj  clojure.spec.test
                :cljs cljs.spec.test)
             :include-macros true
             :as stest]
            [#?(:clj  clojure.spec.gen
                :cljs cljs.spec.impl.gen)
             :as gen]
            [cemerick.url :refer [url-encode]]
            [com.stuartsierra.component :as component]
            [#?(:clj datomic.api
                :cljs datascript.core) :as d]
            #?(:clj [environ.core :refer [env]])
            [plumbing.core #?@(:clj  [:refer :all]
                               :cljs [:refer [map-vals map-keys update-in-when]
                                      :refer-macros [?> ?>> for-map <-]])]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

#?(:cljs (enable-console-print!))

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
     (s/def ::storage #{"mem" "dev" "ddb" "ddb-local"})
     (s/def ::host string?)
     (s/def ::port pos-int?)
     (s/def ::region string?)
     (s/def ::table string?)
     (s/def ::db-name string?)
     (s/def :com.aws/aws-access-key-id string?)
     (s/def :com.aws/aws-secret-key string?)

     (defmulti datomic-uri :storage)

     (defmulti storage-params :storage)

     (s/def ::location (s/multi-spec storage-params :storage))

     (s/fdef datomic-uri
             :args (s/cat :params ::location)
             :ret string?)

     (defmethod datomic-uri "mem"
       [{:keys [storage
                db-name]}]
       (-> "datomic:%s://%s"
           (format storage (url-encode db-name))))

     (defmethod storage-params "mem"
       [_]
       (s/keys :req-un [::storage
                        ::db-name]))

     (defmethod datomic-uri "dev"
       [{:keys [storage
                host
                port
                db-name]
         :or {host "localhost"
              port 4334}}]
       (-> "datomic:%s://%s:%s/%s"
           (format storage host port (url-encode db-name))))

     (defmethod storage-params "dev"
       [_]
       (s/keys :req-un [::storage
                        ::db-name]
               :opt-un [::host
                        ::port]))

     (defn uri-params
       [params]
       (->> params
            (map-keys (comp #(str/replace % "-" "_") name))
            (map (partial str/join "="))
            (str/join "&")
            (str "?")))

     (defn add-aws-creds
       [uri params]
       (or (some-> params
                   (select-keys [:aws-access-key-id
                                 :aws-secret-key])
                   seq
                   (uri-params)
                   (->> (str uri)))
           uri))

     (defmethod datomic-uri "ddb"
       [{:keys [storage
                region
                table
                db-name]
         :as params}]
       (-> "datomic:%s://%s/%s/%s"
           (format storage region table (url-encode db-name))
           (add-aws-creds params)))

     (defmethod storage-params "ddb"
       [_]
       (s/keys :req-un [::storage
                        ::region
                        ::table
                        ::db-name]
               :opt-un [:com.aws/aws-access-key-id
                        :com.aws/aws-secret-key]))

     (defmethod datomic-uri "ddb-local"
       [{:keys [storage
                host
                port
                table
                db-name]
         :as params}]
       (-> "datomic:%s://%s:%s/%s/%s"
           (format storage host port table (url-encode db-name))
           (add-aws-creds params)))

     (defmethod storage-params "ddb-local"
       [_]
       (s/keys :req-un [::storage
                        ::host
                        ::port
                        ::table
                        ::db-name]
               :opt-un [:com.aws/aws-access-key-id
                        :com.aws/aws-secret-key]))))

#?(:cljs
   (do
     (defonce -conns (atom {}))))

(defn connect!
  ([] (connect! #?(:clj  (datomic-uri {:storage "dev"
                                       :db-name "datohm"})
                   :cljs "datohm")))

  ([uri]
   (log/info "connecting to datomic: " uri)
   #?(:clj
      (do
        (d/create-database uri)
        (d/connect uri))
      :cljs
      (connect! uri {})))
  #?(:cljs
     ([uri schema]
      (or (get @-conns uri)
          (let [conn (d/create-conn schema)]
            (swap! -conns assoc uri conn)
            conn)))))

;; ========== Protocols ==========

(defprotocol DatomicUri
  (url [_] [_ db-name]))

(defprotocol DatabaseConnection
  (as-conn [_]))

(defprotocol DatabaseReference
  (as-db [_]))

(extend-protocol DatabaseConnection
  #?(:clj  datomic.Connection
     :cljs cljs.core.Atom)
  (as-conn [c]
    c)
  #?(:clj  clojure.lang.IPersistentMap
     :cljs cljs.core/PersistentArrayMap)
  (as-conn [env]
    (let [{:keys [state conn]} env]
      (assert (or conn state) "env missing :conn")
      (or conn state)))
  #?@(:cljs
      [cljs.core/PersistentHashMap
       (as-conn [env]
                (let [{:keys [state conn]} env]
                  (assert (or conn state) "env missing :conn")
                  (or conn state)))]))

(defn gen-conn
  []
  (gen/return (connect! #?(:clj (datomic-uri {:storage "dev"
                                              :db-name "clojure.spec"})
                           :cljs "clojure.spec"))))

(s/def :datomic/conn
  (s/with-gen conn?
    gen-conn))

(s/def :datohm/conn
  (s/with-gen #(satisfies? DatabaseConnection %)
    #(s/gen (s/or :conn :datomic/conn
                  :env (s/keys :req-un [:datomic/conn])))))

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
        (as-db (as-conn env))))
  #?@(:cljs
      [cljs.core/PersistentHashMap
       (as-db [{:keys [tx/mode tx/result] :as env}]
              (or (and (= :tx.mode/with mode)
                       (:db-after result))
                  (as-db (as-conn env))))]))

(defn gen-db
  []
  (gen/fmap d/db (gen-conn)))

(s/def :datomic/db
  (s/with-gen db? gen-db))

(s/def :datohm/db
  (s/with-gen #(satisfies? DatabaseReference %)
    #(s/gen (s/or :conn :datomic/conn
                  :db :datomic/db
                  :env (s/keys :req-un [:datomic/conn])))))

(s/fdef as-db
        :args (s/cat :db :datohm/db)
        :ret db?)

#?(:clj
   (do
     (s/def ::ephemeral? (s/nilable boolean?))

     (defrecord DatomicDatabase
         [location ephemeral?]
       component/Lifecycle
       (start [this]
         (d/create-database (datomic-uri location))
         this)
       (stop [this]
         (d/delete-database (datomic-uri location))
         (d/shutdown false)
         this)
       DatomicUri
       (url [this]
         (datomic-uri location))
       (url [this db-name]
         (datomic-uri (assoc location :db-name db-name))))

     (def new-datomic-database
       (-> map->DatomicDatabase
           (ctr/wrap-un)
           (ctr/wrap-kargs)))

     (s/fdef new-datomic-database
             :args (s/cat :opts (s/keys :req-un [::location]
                                        :opt-un [::ephemeral?]))
             :ret :datohm/db)

     (defrecord DatomicConnection
         [db conn]
       component/Lifecycle
       (start [this]
         (assoc this :conn (connect! (url db))))
       (stop [this]
         (update-in-when :conn d/release))
       DatabaseReference
       (as-db [this]
         (as-db (as-conn this)))
       DatabaseConnection
       (as-conn [this]
         (:conn this)))

     (def new-datomic-connection
       (-> map->DatomicConnection
           (ctr/wrap-using [:db])
           (ctr/wrap-un)
           (ctr/wrap-kargs)))

     (s/fdef new-datomic-connection
             :args (s/cat :opts (s/nilable (s/keys :opt-un [:datohm/db
                                                            :datohm/conn])))
             :ret :datohm/conn)))
