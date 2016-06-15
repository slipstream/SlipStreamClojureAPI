(ns sixsq.slipstream.client.api.impl.crud
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.utils.http-sync :as http]
    [sixsq.slipstream.client.api.utils.utils :as u]
    #?(:clj [clojure.data.json :as json])))


(def as-json {:accept "application/json"})
(def as-xml {:accept "application/xml"})
(def base-http-params {:insecure? true})
(def param-req-params (conj
                        base-http-params
                        {:content-type "text/plain"
                         :accept       "text/plain;charset=utf-8"
                         :query-params {:ignoreabort "true"}}))
(def scale-req-params (merge param-req-params as-json))

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
    (str (:code e) ". " (:reason e) ". " (:detail e))))

(defn reason-from-exc
  [ex]
  (-> ex
      :body
      #?(:clj  (json/read-str)
         :cljs (JSON.parse))
      u/keywordize-keys
      reason-from-error))

(defn parse-ex-412
  [ex]
  (if (= (:status (ex-data ex)) 412)
    ""
    (throw ex)))

(defn http-creds
  "Throw if credentials are not provided."
  [conf]
  (let [{:keys [username password cookie]} conf]
    (cond
      cookie {:headers {:cookie cookie}}
      (and username password) {:basic-auth [username password]}
      :else (throw (ex-info "Cookie or user credentials are required." {})))))

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


