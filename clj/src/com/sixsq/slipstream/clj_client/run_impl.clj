(ns com.sixsq.slipstream.clj-client.run-impl
  (:refer-clojure :exclude [get])
  (:require
    [com.sixsq.slipstream.clj-client.utils :as u]
    [com.sixsq.slipstream.clj-client.http-impl :as http]
    [superstring.core :as s]
    [clj-json.core :as json]))

(def ^:const global-ns "ss")

(def ^:const comp-prop-sep ":")
(def ^:const comp-mult-sep ".")

(def ^:const state-param (str global-ns comp-prop-sep "state"))
(def ^:const abort-param (str global-ns comp-prop-sep "abort"))

(def ^:const non-scalable-final-states ["Finalizing" "Done"])
(def ^:const scalable-states ["Ready"])


(defn run-uri
  [run-uuid]
  (u/url-join ["run" run-uuid]))

(defn run-url
  [service-url run-uuid]
  (u/url-join [service-url (run-uri run-uuid)]))

(defn run-state-uri
  [run-uuid]
  (u/url-join [(run-uri run-uuid) state-param]))

(defn run-state-url
  [service-url run-uuid]
  (u/url-join [service-url (run-state-uri run-uuid)]))

(defn run-abort-uri
  [run-uuid]
  (u/url-join [(run-uri run-uuid) abort-param]))

(defn run-abort-url
  [service-url run-uuid]
  (u/url-join [service-url (run-abort-uri run-uuid)]))


;; Default set of http request parameters.
(def as-json {:accept "application/json"})
(def as-xml {:accept "application/xml"})
(def base-http-params {:insecure? true})
(def param-req-params (conj
                        base-http-params
                        {:content-type "text/plain"
                         :accept       "text/plain;charset=utf-8"
                         :query-params {:ignoreabort "true"}}))
(def scale-req-params (merge param-req-params as-json))

(defn to-param
  "Retruns parameter as 'comp.id:param' or 'comp:param' if 'id' is nil."
  [comp id param]
  (if id
    (str comp comp-mult-sep id comp-prop-sep param)
    (str comp comp-prop-sep param)))

(defn to-param-url
  "Returns parameter full URL as 'comp.id:param' or
  'comp:param if 'id' is nil."
  [service-url run-uuid comp id param]
  (u/url-join [(run-url service-url run-uuid) (to-param comp id param)]))

(defn to-param-uri
  "Returns parameter full URI as 'run/run-uuid/comp.id:param' or
  'run/run-uuid/comp:param if 'id' is nil."
  [run-uuid comp id param]
  (u/url-join [(run-uri run-uuid) (to-param comp id param)]))

(defn to-component-uri
  [run-uuid comp]
  (u/url-join [(run-uri run-uuid) comp]))

(defn to-component-url
  [service-url run-uuid comp]
  (u/url-join [(run-url service-url run-uuid) comp]))

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
  (throw (ex-info "HTTP Error 409 - Conflict."
                  {:status  409
                   :headers {}
                   :body    body-409})))

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

(defn http-creds
  "Throw if credentials are not provided."
  [conf]
  (let [{:keys [username password cookie]} conf]
    (cond
      cookie {:headers {:cookie cookie}}
      (and username password) {:basic-auth [username password]}
      :else (throw (Exception. "Cookie or user credentials are required.")))))

(defn parse-ex-412
  [ex]
  (if (= (:status (ex-data ex)) 412)
    ""
    (throw ex)))

(defn map-merge
  [a b]
  (if (and (map? a) (map? b))
    (merge-with map-merge a b)
    b))

(defn merge-request
  [& maps]
  (apply merge-with map-merge maps))

(defn with-default-request
  [& maps]
  (apply merge-request param-req-params maps))

(defn to-url
  [conf uri]
  (u/url-join [(:serviceurl conf) uri]))

;;
;; CRUD requests.
(defn get
  [uri conf & [req]]
  (:body (http/get
           (to-url conf uri)
           (with-default-request
             req
             (http-creds conf)))))

(defn put
  [uri body conf & [req]]
  (http/put
    (to-url conf uri)
    (with-default-request
      req
      (http-creds conf)
      {:body body})))

(defn delete
  [uri conf & [req body-params]]
  (http/delete
    (to-url conf uri)
    (with-default-request
      req
      (http-creds conf)
      (if (seq body-params)
        {:body (u/to-body-params body-params)}))))

(defn post
  [uri conf & [req body-params]]
  (http/post
    (to-url conf uri)
    (with-default-request
      req
      (http-creds conf)
      (if (seq body-params)
        {:body (u/to-body-params body-params)}))))


