(ns datohm.deploy
  {:boot/export-tasks true}
  (:require [datohm.config :as config]
            [pleasetrythisathome.build :refer :all]
            [boot.core :refer :all]
            [boot.task.built-in :refer :all]
            [boot.file :as file]
            [boot.pod  :as pod]
            [boot.util :as util]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(deftask template-datomic
  "template datomic deploy files"
  [p project NAME str "name of the project"
   e env VAL kw "deploy environment ie. staging"]
  (let [config (config/config project env)
        subs (->> config
                  (merge {:project project
                          :env (name env)
                          :datomic-version (dep-version 'com.datomic/datomic-pro)})
                  (config/jsonify-keys))
        template (r adzerk.boot-template/template)]
    (comp
     (template :paths ["datomic/ddb-transactor.properties"
                       "datomic/cf.properties"]
               :subs subs)
     (target))))

(deftask deploy-datomic
  ""
  [p project NAME str "name of the project"
   e env VAL kw "deploy environment ie. staging"]
  (let [config (config/config "datohm" env)]
    (with-post-wrap fileset
      (util/info (str/join " " ["sh" "scripts/deploy-transactor.sh"
                            "-v" (dep-version 'com.datomic/datomic-pro)
                            "-p" project
                            "-e" (name env)
                            "-r" (get-in config [:transactor :aws-region])]))
      #_(util/info (pr-str (:out (sh/sh "sh" "scripts/deploy-transactor.sh"
                               "-v" (dep-version 'com.datomic/datomic-pro)
                               "-p" project
                               "-e" (name env)
                               "-r" (get-in config [:transactor :aws-region])))))
      fileset)))
