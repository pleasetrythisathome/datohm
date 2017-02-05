(load-file "build/build.clj")
(require '[clojure.edn :as edn])
(def deps (edn/read-string (slurp "build/deps.edn")))

(def project "datohm")
(def version (deduce-version-from-git))

(set-env!
 :dependencies #(vec
                 (concat
                  %
                  '[[ch.qos.logback/logback-classic  "1.0.1"]]
                  (->> [:clojure
                        :clojurescript
                        :datascript
                        :datomic
                        :env
                        {:http [:aleph
                                :bidi]}
                        :logging
                        :manifold
                        :plumbing
                        :transit
                        :time]
                       (pull-deps deps))
                  (->> [{:boot [:component
                                :datomic
                                :laces
                                :test]
                         :test [:check]}]
                       (pull-deps deps "test"))))
 :source-paths #{"src"}
 :resource-paths #{"resources"})

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-test :refer [test]]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl]
 '[environ.core :refer [env]]
 'datomic.codec
 'datomic.db
 'datomic.function
 '[tailrecursion.boot-datomic :refer [datomic]])

(bootlaces! version)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A batteries included om.next framework"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :scm {:url (str "https://github.com/pleasetrythisathome/" project)}}
 datomic {:license-key (env :datomic-license)})

(def datomic-data-readers
  {'base64 datomic.codec/base-64-literal
   'db/id  datomic.db/id-literal
   'db/fn  datomic.function/construct})

(deftask test-clj
  "test cljs"
  []
  (set-env! :source-paths #(conj % "test"))
  (comp
   (test)))

(deftask dev
  "watch and compile css, cljs, init cljs-repl and push changes to browser"
  []
  (set-env! :source-paths #(conj % "test" "dev"))
  (apply repl/set-refresh-dirs (get-env :directories))
  (comp
   (datomic)
   (watch)
   (repl :server true)
   (notify)
   (target)))
