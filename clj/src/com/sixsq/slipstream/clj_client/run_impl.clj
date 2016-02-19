(ns com.sixsq.slipstream.clj-client.run-impl
  (:require
    [com.sixsq.slipstream.clj-client.config :as c]
    [com.sixsq.slipstream.clj-client.utils :as u]
    [superstring.core :as s]
    [clj-json.core :as json])
  (:use [slingshot.slingshot :only [throw+]]))

(def ^:const run-uri "run")
(def ^:const global-ns "ss")
(def ^:const comp-mult-sep ".")

(def ^:const comp-prop-sep ":")
(def ^:const state-param (str global-ns comp-prop-sep "state"))
(def ^:const abort-param (str global-ns comp-prop-sep "abort"))
(def ^:const non-scalable-final-states ["Finalizing" "Done"])
(def ^:const scalable-states ["Ready"])

(def config (:contextualization (c/get-config)))
(def cookie (:cookie config))
(def run-url (u/url-join [(:serviceurl config) run-uri (:diid config)]))
(def run-state-url (u/url-join [run-url state-param]))
(def run-abort-url (u/url-join [run-url abort-param]))

;; Default set of http request parameters.
(def base-http-params {:insecure? true
                      :headers    {:cookie cookie}})
(def param-req-params (conj
                      base-http-params
                      {:content-type "text/plain"
                       :accept       "text/plain;charset=utf-8"
                       :query-params {:ignoreabort "true"}}))
(def scale-req-params (assoc param-req-params :accept "application/json"))

(defn build-param
  "Retruns parameter as 'comp.id:param' or 'comp:param' if 'id' is nil."
  [comp id param]
  (if id
    (str comp comp-mult-sep id comp-prop-sep param)
    (str comp comp-prop-sep param)))

(defn build-param-url
  "Returns parameter full URL as 'comp.id:param' or
  'comp:param if 'id' is nil."
  [comp id param]
  (u/url-join [run-url (build-param comp id param)]))

(defn build-component-url
  [comp]
  (u/url-join [run-url comp]))

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

