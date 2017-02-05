(load-file "build/build.clj")
(require '[clojure.edn :as edn])
(def deps (edn/read-string (slurp "build/deps.edn")))

(def project "datohm")
(def version (deduce-version-from-git))

(set-env!
 :repositories #(conj % ["my.datomic.com" {:url "https://my.datomic.com/repo"
                                           :username (get-sys-env "DATOMIC_USER")
                                           :password (get-sys-env "DATOMIC_PASS")}])
 :dependencies #(vec
                 (concat
                  %
                  (->> [:clojure
                        :clojurescript
                        :datomic
                        :datascript
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
                                :laces
                                :test]
                         :test [:check]}]
                       (pull-deps deps "test")))))

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-test :refer [test]]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl])

(bootlaces! version)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A batteries included om.next framework"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :scm {:url (str "https://github.com/pleasetrythisathome/" project)}})

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
   (watch)
   (repl :server true)
   (notify)
   (target)))
