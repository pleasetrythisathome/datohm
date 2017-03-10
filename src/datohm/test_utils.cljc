(ns datohm.test-utils
  (:require [#?(:clj clj-time.core :cljs cljs-time.core) :as time]
            [#?(:clj clj-time.format :cljs cljs-time.format) :as timef]
            [#?(:clj clj-time.coerce :cljs cljs-time.coerce) :as timec]
            [#?(:clj  clojure.spec
                :cljs cljs.spec)
             :as s]
            [#?(:clj  clojure.spec.gen
                :cljs cljs.spec.impl.gen)
             :as gen]
            [#?(:clj  clojure.spec.test
                :cljs cljs.spec.test)
             :include-macros true
             :as stest]
            [#?(:clj  clojure.test
                :cljs cljs.test)
             :as t
             :include-macros true
             #?(:clj  :refer
                :cljs :refer-macros)
             [deftest testing is]]
            [clojure.string :as str]
            [clojure.test.check :as stc]
            [clojure.test.check.properties])
  #?(:cljs
     (:require-macros [datohm.test-utils :refer [time-tests spec-problems]])))

(stest/instrument)

(def check-opts
  {:clojure.spec.test.check/opts {:num-tests 25}})

(defn now []
  #?(:clj  (timec/to-long (time/now))
     :cljs (if-let [now (.-now (.-performance js/window))]
             (.call now (.-performance js/window))
             (.now js/Date))))

#?(:clj
   (defmacro time-tests
     [sym & tests]
     `(testing ~sym
        (let [start# (datohm.test-utils/now)]
          ~@tests
          (let [t# (- (datohm.test-utils/now) start#)]
            (println (str "time: " t# " " ~sym)))))))

#?(:clj
   (defmacro spec-problems
     [form]
     `(try ~form
           nil
           (catch Throwable e#
             (:clojure.spec/problems (ex-data e#))))))

(defn gen
  [spec]
  (try (gen/generate (s/gen spec))
       (catch #?(:clj Throwable :cljs js/Error) e
         (ex-data e))))

(defn passed?
  [checked]
  (cond
    (map? checked) (not (:failure checked))
    :else true))

(defn specs
  ([] (specs nil))
  ([ns]
   (for [[k spec] (s/registry)
         :when (and (not (re-find #"clojure" (namespace k)))
                    (or (not ns)
                        (re-find (re-pattern ns) (namespace k))))]
     k)))

(defmacro test-checkable-syms
  [syms]
  `(testing "test-checkable-syms"
     (doall
      (for [sym# (sort (filter symbol? ~syms))]
        (datohm.test-utils/time-tests
         sym#
         (is (-> sym#
                 (stest/check datohm.test-utils/check-opts)
                 first
                 datohm.test-utils/passed?)))))))

(-> 'datohm.conn/new-datomic-connection
    (stest/check datohm.test-utils/check-opts)
    first
    ;;datohm.test-utils/passed?
    )
