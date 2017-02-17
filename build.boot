(load-file "build/build.clj")
(require '[clojure.edn :as edn])
(def deps (edn/read-string (slurp "build/deps.edn")))

(def project "datohm")
(def version (deduce-version-from-git))

(set-env!
 :dependencies #(vec
                 (concat
                  %
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
                  (->> [{:boot [:cljs
                                :cljs-repl
                                :cljs-test
                                :component
                                :datomic
                                :laces
                                :reload
                                :test]
                         :test [:check]}]
                       (pull-deps deps "test"))))
 :source-paths #{"src"}
 :resource-paths #{"resources"})

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[adzerk.boot-test :refer [test]]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl]
 '[crisptrutski.boot-cljs-test :as ctest]
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
   (datomic)
   (test)))

(deftask test-cljs
  "test all"
  []
  (set-env! :source-paths #(conj % "test"))
  (comp
   (ctest/test-cljs)))

(deftask test-all
  "test all"
  []
  (set-env! :source-paths #(conj % "test"))
  (comp
   (test-clj)
   (test-cljs)))

(deftask dev
  "watch and compile css, cljs, init cljs-repl and push changes to browser"
  []
  (set-env! :source-paths #(conj % "test" "dev"))
  (apply repl/set-refresh-dirs (get-env :directories))
  (comp
   (datomic)
   (watch)
   (notify)
   (reload :port 3459)
   (cljs-repl :port 3458)
   (cljs :source-map true)
   (target)))
