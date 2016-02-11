(ns com.sixsq.slipstream.clj-client.config
  (:require
    [com.sixsq.slipstream.clj-client.utils :as u]
    [clojure.java.io :as io]
    [clojure.tools.logging  :as log]
    [clojure-ini.core :refer [read-ini]]))


;;
;; Location defaults.

(def ^:private ^:const ss-client-home
  (->> ["opt" "slipstream" "client"]
       u/path-join
       u/force-absolute))

;;
;; Configuration.

(def ^:private ^:const context-fn "slipstream.context")

(def ^:private ^:const  context-file-locs
  [(System/getProperty  "user.dir")
   (System/getProperty  "user.home")
   (u/path-join         [ss-client-home "bin"])
   (u/path-join         [ss-client-home "sbin"])
   (System/getProperty  "java.io.tmpdir")])

(def ^:private resource-context
  (if-let [f (io/resource context-fn)] (.getPath f)))

(defn context-file-paths
  []
  (->> context-file-locs
       (map (partial u/path-append context-fn))
       (concat [resource-context])
       (remove nil?)))

(defn- file-exists?
  [file-name]
  (.exists (io/as-file file-name)))

(defn- find-conf-file []
  (->> (context-file-paths)
       (filter file-exists?)
       first))

(def read-conf
  (delay
    (if-let [conf (find-conf-file)]
      (do
        (log/info "found conf: " conf)
        (read-ini conf
          :keywordize? true
          :comment-char \#))
      (throw (IllegalStateException. "Failed to find configuration file.")))))

(defn get-config
  []
  @read-conf)
