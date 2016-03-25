(ns com.sixsq.slipstream.clj-client.lib.utils
  (:require [superstring.core :as s]
            [clojure.walk :as walk]))

(defn in?
  [x xs]
  (boolean ((set xs) x)))

(defn url-join
  "Trivial joiner of a sequence on '/'.
  Not meant to be following RFC 3986.
  "
  [& [parts]]
  (s/join "/" parts))

(defn to-body-params
  [query-map & [on]]
  (s/join (or on "\n") (map #(s/join "=" %)
                            (remove #(s/blank? (first %))
                                    (walk/stringify-keys query-map)))))

(defn split
  [s on]
  (s/split s on))

(defn keywordize-keys
  [d]
  (walk/keywordize-keys d))
