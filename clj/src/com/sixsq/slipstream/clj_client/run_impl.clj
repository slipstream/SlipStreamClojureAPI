(ns com.sixsq.slipstream.clj-client.run-impl
  (:require
    [com.sixsq.slipstream.clj-client.config :as c]
    [com.sixsq.slipstream.clj-client.utils :as u]
    [superstring.core :as s]
    [clj-json.core :as json])
  (:use [slingshot.slingshot :only [throw+]]))

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
                      :headers   {:cookie cookie}})
(def rtp-req-params (conj
                      base-req-params
                      {:content-type "text/plain"
                       :accept       "text/plain;charset=utf-8"
                       :query-params {:ignoreabort "true"}}))
(def scale-req-params (assoc rtp-req-params :accept "application/json"))

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
  (->> (s/trim s)
       (re-seq #"\.(\d*$)")
       first
       last))

(defn extract-ids
  [names]
  (->> (if (= java.lang.String (type names)) (s/split names #",") names)
       (map extract-id)
       (remove nil?)))

(def ^:private body-409
  "{\"error\": {
                \"code\": \"409\",
                \"reason\": \"Conflict\",
                \"detail\": \"Abort flag raised!\"}}")

(defn throw-409
  []
  (throw+ {:status  409
           :headers {}
           :body    body-409}))

(defn- reason-from-error
  [error]
  (let [e (:error error)]
    (format "%s. %s. %s" (:code e) (:reason e) (:detail e))))

(defn reason-from-exc
  [ex]
  (-> ex
      :body
      json/parse-string
      u/keywordize-keys
      reason-from-error))

