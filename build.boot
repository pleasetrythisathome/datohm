(set-env!
 :source-paths #{"build/src" "src"}
 :resources-paths #{"build/resources" "resources"})

(require '[pleasetrythisathome.build :refer :all]
         '[pleasetrythisathome.deps :refer [deps]])

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
                                :component
                                :datomic
                                :laces
                                :reload]
                         :test [:check]}]
                       (pull-deps deps)
                       (scope-as "test"))))
 :source-paths #{"src"}
 :resource-paths #{"resources"})

(require
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl]
 '[environ.core :refer [env]]
 '[tailrecursion.boot-datomic :refer [datomic]])

(bootlaces! version)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A batteries included om.next framework."
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :scm {:url (str "https://github.com/pleasetrythisathome/" project)}}
 cider {:cljs true}
 datomic {:license-key (env :datomic-license)})

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
