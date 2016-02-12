(ns com.sixsq.slipstream.clj-client.run-util
  (:require
    [com.sixsq.slipstream.clj-client.config :as c]
    [com.sixsq.slipstream.clj-client.utils :as u]
    [superstring.core :as s]
    [clojure-ini.core :refer [read-ini]]))

(def ^:const run-uri "run")
(def ^:const global-ns "ss")
(def ^:const node-mult-sep ".")

(def ^:const node-prop-sep ":")
(def ^:const state-rtp (str global-ns node-prop-sep "state"))
(def ^:const abort-rtp (str global-ns node-prop-sep "abort"))
(def ^:const non-scalable-final-states ["Finalizing" "Done"])
(def ^:const scalable-states ["Ready"])

(def config (:contextualization (c/get-config)))
(def cookie (:cookie config))
(def run-url (u/url-join [(:serviceurl config) run-uri (:diid config)]))
(def run-state-url (u/url-join [run-url state-rtp]))
(def run-abort-url (u/url-join [run-url abort-rtp]))

;; Default set of request parameters.
(def base-req-params {:insecure? true
                      :headers {:cookie cookie}})

(defn build-rtp
  [node-name id param]
  (if id
    (str node-name node-mult-sep id node-prop-sep param)
    (str node-name node-prop-sep param)))

(defn build-rtp-url
  [node-name id param]
  (u/url-join [run-url (build-rtp node-name id param)]))

(defn build-node-url
  [node-name]
  (u/url-join [run-url node-name]))

(defn- extract-id
  [s]
  (->>  (s/trim s)
        (re-seq #"\.(\d*$)")
        first
        last))

(defn extract-ids
  [names]
  (->>  (if (= java.lang.String (type names)) (s/split names #",") names)
        (map extract-id)
        (remove nil?)))