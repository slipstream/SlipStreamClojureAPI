(ns sixsq.slipstream.client.api.utils.wait
  (:require
    #?(:clj [clojure.core.async :refer [timeout go-loop <! <!!]]
       :cljs [cljs.core.async :refer [timeout <!]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go-loop]])))

(def default_interval 1)
(defn- validate-interval [interval]
  (if (pos? interval)
    interval
    default_interval))

(defn iterations [timeout interval]
  (-> (quot timeout (validate-interval interval))
      (max 1)))

(defn wait-for-async
  "Asynchronous wait for 'predicate' for 'time-out' seconds with
   'interval' seconds.  Returns channel that will contain the returned value."
  {:doc/format :markdown}
  [predicate time-out interval]
  (let [interval    (validate-interval interval)
        max-n       (iterations time-out interval)
        interval-ms (* 1000 interval)]
    (go-loop [n 1]
      (if-let [result (predicate)]
        result
        (do
          (<! (timeout interval-ms))
          (if (< n max-n)
            (do
              (recur (inc n)))))))))

#?(:clj
   (defn wait-for
     "Wait for 'predicate' for 'time-out' seconds with 'interval' seconds."
     {:doc/format :markdown}
     [predicate time-out interval]
     (<!! (wait-for-async predicate time-out interval))))