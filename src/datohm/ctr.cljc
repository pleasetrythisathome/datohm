(ns datohm.ctr
  (:require [com.stuartsierra.component :as component]
            [plumbing.core :refer [map-keys]]))

(defn wrap-kargs
  "call f with a {} of options
  (f {:arg 0}), (f :arg 0)
  ;; => (f {:arg 0})
  (f) (f nil)
  ;; => (f {})"
  [f]
  (fn kargs
    ([] (kargs {}))
    ([a b & {:as opts}]
     (kargs (assoc opts a b)))
    ([opts]
     (f (or opts {})))))

(defn wrap-defaults
  "call (f opts) with (merge default-opts opts)"
  [f default-opts]
  (fn [opts]
    (f (merge (or default-opts {}) opts))))

(defn wrap-un
  [f]
  (fn [opts]
    (f (map-keys (comp keyword name) opts))))

(defn wrap-using
  [f using]
  (fn [opts]
    (-> (f opts)
        (component/using using))))
