(ns user
  (:require [datohm.conn :as conn]
            [#?(:clj clojure.pprint
                :cljs cljs.pprint)
             :refer [pprint]]
            [taoensso.timbre :as log]))

#?(:clj
   (do
     ;; (boot.core/load-data-readers!)

     (defn cljs-repl
       "Start a ClojureScript REPL"
       []
       (eval
        '(do (in-ns 'boot.user)
             (start-repl))))

     (def conn (conn/connect "datohm"))
     (def env {:conn conn})
     ))

(comment

  )
