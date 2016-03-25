(ns com.sixsq.slipstream.clj-client.lib.utils.context
  (:require [superstring.core :as s])
  (:import [java.io File]))

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
