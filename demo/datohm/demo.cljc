(ns datohm.demo
  (:require [#?(:clj  taoensso.timbre
                :cljs shodan.console)
             :as log]))

#?(:cljs
   (defn main
     []
     (log/info {:greeting "hello world!"})))
