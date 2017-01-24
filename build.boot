(load-file "build/build.clj")
(require '[clojure.edn :as edn])
(def deps (edn/read-string (slurp "build/deps.edn")))

(def project "dohm")
(def version (deduce-version-from-git))

(set-env!
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
                        :transit
                        :time]
                       (pull-deps deps))
                  (->> [{:test [:check]}]
                       (pull-deps deps "test")))))

(require
 '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A batteries include om.next framework"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :scm {:url (str "https://github.com/pleasetrythisathome/" project)}})
