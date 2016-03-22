(ns com.sixsq.slipstream.clj-client.lib.utils.wait
  (:require
    #?(:clj  [clojure.core.async :refer [go go-loop <!]]
       :cljs [cljs.core.async :refer [<!]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]])))

(defn iterations [timeout interval]
  (-> (quot timeout interval)
      inc
      max 1))

(defn wait-for
  "Wait for 'predicate' for 'timeout' seconds with 'interval' seconds."
  [predicate timeout interval]
  (let [max-n (iterations timeout interval)
        interval-ms (* 1000 interval)]
    (go-loop [n 1]
      (if-let [result (predicate)]
        result
        (do
          (<! (timeout interval-ms))
          (if (< n max-n)
            (recur (inc n))))))))
