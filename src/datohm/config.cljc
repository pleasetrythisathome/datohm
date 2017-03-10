(ns datohm.config
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [taoensso.encore :refer [merge-deep]]
            [taoensso.timbre :as log]
            #?@
            (:clj
             [[aero.core :as aero]
              [clojure.java.io :as io]
              [environ.core :refer [env]]]))
  #?(:cljs
     (:require-macros [datohm.config :refer [cljs-config profile]])))

(defn korks->ks
  [kork]
  (if (keyword? kork)
    (vector kork)
    kork))

(defn filter-cljs
  [config]
  (let [ks (or (:cljs (meta config)) [])]
    (->> ks
         (map korks->ks)
         (reduce (fn [out ks]
                   (assoc-in out ks (get-in config ks)))
                 {}))))

(defn walk-keys
  [f m]
  (let [f (fn [[k v]]
            (if (keyword? k)
              [(f k) v]
              [k v]))]
    (walk/postwalk (fn [x]
                     (if (map? x)
                       (into {} (map f x))
                       x))
                   m)))

(defn jsonify
  [k]
  (str/replace (name k) #"-" "_"))

(defn jsonify-keys
  [m]
  (walk-keys jsonify m))

#?(:clj
   (do
     (defn config-from
       [file profile]
       (or (try (aero/read-config file {:profile profile})
                (catch Throwable e
                  (log/warn "failed reading config from" file e)))
           {}))

     (defn config-from-classpath
       [project profile]
       (-> project
           (str ".edn")
           io/resource
           (config-from profile)))

     (defn user-config
       [project profile]
       (let [file (str "." project ".edn")
             f (-> "user.home"
                   System/getProperty
                   (io/file file))]
         (if (.exists f)
           (config-from f profile)
           (do
             (log/info (format "no config found at ~/%s" file))
             {}))))

     (defn config
       ([project] (config project "dev"))
       ([project profile]
        (merge-deep (config-from-classpath project profile)
                    (user-config project profile))))

     (defmacro profile
       []
       (env :profile :dev))

     (defmacro cljs-config
       [profile]
       (->> (config profile)
            (filter-cljs))))
   :cljs
   (defn config
     [profile]
     (cljs-config profile)))
