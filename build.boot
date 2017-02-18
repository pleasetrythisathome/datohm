(set-env!
 :source-paths #{"build/src" "src"}
 :resources-paths #{"build/resources" "resources"})

(require '[pleasetrythisathome.build :refer :all]
         '[pleasetrythisathome.deps :refer [deps]])

(def org "pleasetrythisathome")
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
                                :devtools
                                :http
                                :laces
                                :reload
                                :template]
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
 '[boot.util :as util]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl]
 '[environ.core :refer [env]]
 '[pandeiro.boot-http :refer [serve]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
 '[tailrecursion.boot-datomic :refer [datomic]])

(bootlaces! version)

(util/info (pr-str [(symbol org project) version]))

(task-options!
 pom {:project (symbol org project)
      :version version
      :description "A batteries included om.next framework."
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :url (format "https://github.com/%s/%s" org project)
      :scm {:url (format "https://github.com/%s/%s" org project)}}
 cider {:cljs true}
 datomic {:license-key (env :datomic-license)})

(deftask dev
  "watch and compile css, cljs, init cljs-repl and push changes to browser"
  []
  (set-env! :source-paths #(conj % "dev"))
  (set-env! :resource-paths #(conj % "html"))
  (comp
   (testing)
   (datomic)
   (serve :dir "target"
          :httpkit true)
   (watch)
   (notify)
   (reload)
   (cljs-repl)
   (cljs :optimizations :none
         :source-map true)
   (target)))
