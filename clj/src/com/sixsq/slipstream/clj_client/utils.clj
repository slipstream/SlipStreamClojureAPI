(ns com.sixsq.slipstream.clj-client.utils
  (:require [superstring.core :as s]
            [clojure.walk :as walk])
  (:import [java.io File]))

(defn in?
  [x xs]
  (boolean ((set xs) x)))

(defn- now-ms []
  (System/currentTimeMillis))

(defn now-sec
  []
  (quot (now-ms) 1000))

(defn force-absolute
  [path]
  (if (.startsWith path File/separator)
    path
    (str File/separator path)))

(defn path-join
  [paths]
  (s/join File/separator paths))

(defn path-append
  [path paths]
  (str paths File/separator path))

(defn wait-for
  "Wait for 'predicate' for 'timeout' seconds with 'interval' seconds."
  [predicate timeout interval]
  (let [stop-time (+ timeout (now-sec))]
    (loop []
      (if-let [result (predicate)]
        result
        (do
          (Thread/sleep (* interval 1000))
          (if (< (now-sec) stop-time)
            (recur)))))))

(defn url-join
  "Trivial joiner of a sequence on '/'.
  Not meant to be following RFC 3986.
  "
  [& [parts]]
  (s/join "/" parts))

(defn split
  [s on]
  (s/split s on))

(defn keywordize-keys
  [d]
  (walk/keywordize-keys d))
