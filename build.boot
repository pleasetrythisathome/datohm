(require '[clojure.edn :as edn])

(def project "dohm")

(def deps
  (edn/read-string (slurp "build/deps.edn")))

(load-file "build/build.clj")

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
