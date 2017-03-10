(set-env!
 :dependencies '[[pleasetrythisathome/boot.build "0.1.0-SNAPSHOT"]])

(require '[pleasetrythisathome.build :refer :all])

(def org "pleasetrythisathome")
(def project "datohm")
(def version (deduce-version-from-git))

(merge-project-env! (project-env))

(merge-env!
 :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                  :username (get-sys-env "DATOMIC_USER")
                                  :password (get-sys-env "DATOMIC_PASS")}}
 :dependencies (->> [{:boot [:cljs
                             :cljs-repl
                             :component
                             :datomic
                             :devtools
                             :http
                             :laces
                             :reload
                             :template
                             :test]}
                     :repl
                     {:test [:check]}]
                    (pull-deps)
                    (scope-as "test")))

(require
 '[datohm.config :as config]
 '[datohm.deploy :refer :all]
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[adzerk.boot-template :as st]
 '[adzerk.boot-test :refer [test]]
 '[boot.util :as util]
 '[boot-component.reloaded :refer :all]
 '[clojure.tools.namespace.repl :as repl]
 '[environ.core :refer [env]]
 '[pandeiro.boot-http :refer [serve]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools dirac]]
 '[tailrecursion.boot-datomic :refer [datomic]])

(bootlaces! version)

(task-options!
 pom {:project (symbol org project)
      :version version
      :description "A batteries included om.next framework."
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :url (format "https://github.com/%s/%s" org project)
      :scm {:url (format "https://github.com/%s/%s" org project)}}
 datomic {:license-key (get-in (config/config "datohm") [:license :datomic])}
 test {:exclusions #{'datohm.deploy}}
 cljs-repl {:nrepl-opts {:middleware '[cider.nrepl/cider-middleware
                                       refactor-nrepl.middleware/wrap-refactor
                                       cemerick.piggieback/wrap-cljs-repl]}})

(deftask dev
  "watch and compile css, cljs, init cljs-repl and push changes to browser"
  []
  (set-env! :source-paths #(conj % "dev" "demo/src")
            :resource-paths #(conj % "demo/resources"))
  (comp
   (testing)
   (datomic)
   (serve :httpkit true)
   (watch)
   (notify)
   (reload)
   (dirac)
   (cljs-devtools)
   (cljs-repl)
   (cljs :optimizations :none
         :source-map true)
   (target)))
