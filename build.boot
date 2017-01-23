(load-file "deps/deps.clj")
(require '[deps :as deps])

(set-env!
 :dependencies (vec
                (concat
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
                      (deps/select-deps deps))
                 (->> [{:boot [:cljs
                               :cljs-repl
                               :cljs-test
                               :laces
                               :reload]}
                       {:test [:check]}]
                      (deps/select-deps deps)
                      (mapv #(conj % :scope "test")))))
 :source-paths #{"src"}
 :resource-paths #(conj % "resources"))

(require
 '[boot.file :as file]
 '[boot.util :as util]
 '[clojure.java.io :as io]
 '[taoensso.encore :refer [merge-deep]]
 '[adzerk.bootlaces :refer :all]
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[adzerk.boot-test :refer [test]]
 '[crisptrutski.boot-cljs-test :as ct]
 '[boot-component.reloaded :refer :all]
 '[environ.core :refer [env]]
 '[org.martinklepsch.boot-garden :refer :all]
 '[pandeiro.boot-http :refer [serve]])

(def +version+ "0.1.0-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 pom {:project 'borderless
      :version +version+
      :description ""
      :url "https://github.com/pleasetrythisathome/borderless"
      :scm {:url "https://github.com/pleasetrythisathome/borderless"}}
 aot {:namespace #{'borderless.main}}
 jar {:main 'borderless.main
      :file "borderless.jar"}
 ct/test-cljs {:js-env :phantom}
 garden {:styles-var 'borderless.style/style
         :output-to "public/css/style.css"
         :vendors ["webkit" "moz" "o"]}
 beanstalk {:name        "borderless"
            :version     +version+
            :description ""
            :access-key  (env :aws-access-key-id)
            :secret-key  (env :aws-secret-key)
            :file        "target/project.zip"
            :stack-name  "64bit Amazon Linux 2016.03 v2.1.0 running Docker 1.9.1"
            :bucket      "hiborderless-deploy"
            :beanstalk-envs [{:name "borderless-dev"
                              :cname-prefix "hiborderless-dev"}
                             {:name "borderless-staging"
                              :cname-prefix "hiborderless-staging"}
                             {:name "borderless-prod"
                              :cname-prefix "hiborderless"}
                             {:name "borderless-backup"
                              :cname-prefix "hiborderless-backup"}]})

(deftask dev
  "watch and compile css, cljs, init cljs-repl and push changes to browser"
  []
  (set-env! :source-paths   #(conj % "test" "dev"))
  (set-env! :resource-paths #(conj % "test" "src"))
  (comp
   (watch)
   (notify)
   (reload :port 3459
           :ip "0.0.0.0"
           ;; :ws-port 443
           ;; :ws-host "borderless-reload.ngrok.io"
           ;; :secure true
           :asset-host "/"
           :on-jsload 'borderless.main/main)
   (cljs-repl :port 3458
              ;; :ws-host "borderless-repl.ngrok.io"
              ;; :secure true
              )
   (reload-system :system-var 'dev/new-development-system
                  :start-var 'borderless.systems/start)
   (garden :pretty-print true)
   (cljs :source-map true)
   (target)))

(deftask package
  "compile cljx, garden, cljs, and build a jar"
  []
  (comp
   (cljs :optimizations :advanced)
   (garden)
   (aot)
   (pom)
   (uber)
   (jar)
   (target)))

(deftask add-file
  "add deployment files to fileset"
  [f path PATH str "the path to the file"
   t target PATH str "the target in the fileset"]
  (let [tgt (tmp-dir!)
        add-files
        (delay
         (let [file (io/file path)
               target (or target (.getName file))]
           (util/info (str "Adding " path " to fileset as " target "...\n"))
           (file/copy-with-lastmod file (io/file tgt target))))]
    (with-pre-wrap fileset
      @add-files
      (-> fileset (add-resource tgt) commit!))))

(deftask build-docker
  "Build my application docker zip file."
  []
  (comp
   (add-repo)
   (add-file :path "target/borderless.jar")
   (dockerrun)
   (zip)
   (target)))
