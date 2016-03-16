(ns com.sixsq.slipstream.clj-client.http-impl
  "Simple wrapper around an HTTP library to produce consistent CRUD interface.

  The CRUD actions accept and produce `Ring`-style requests/responses.

  Examples.

  GET using Basic authn
  req := {:accept :xml
          :basic-auth [\"user\" \"password\"]}
  resp := {:status 200
           :body \"<Hello :)>\"
           :headers {...}}

  GET using cookie
  req := {:accept :json
          :headers {:cookie cookie}}
  resp := {:status 200
           :body \"{\"Hello\": \":)\"}\"
           :headers {...}}

  ```clojure
  (:body (get \"https://httpbin.org/get\"))
  ```

  On HTTP error, thows ExceptionInfo with `:data` containing the full response.
  The full response can be obtained with `(ex-data ex)`

  ```clojure
  (let [{:keys [status body]}
        (try
          (get \"https://httpbin.org/error\")
          (catch ExceptionInfo e (ex-data e)))
  ```
  "
  (:refer-clojure :exclude [get])
  (:require [kvlt.core :as kvlt]))

(defn- request!
  [meth url req]
  (try
    (-> (merge {:method (keyword meth) :url url} req)
        kvlt/request!
        deref)))

(defn get
  [url & [req]]
  (request! :get url req))

(defn put
  [url & [req]]
  (request! :put url req))

(defn post
  [url & [req]]
  (request! :post url req))

(defn delete
  [url & [req]]
  (request! :delete url req))


