(ns sixsq.slipstream.client.api.utils.http
  "Simple synchronous wrapper around an HTTP library to produce consistent
  CRUD interface.

  The CRUD actions accept and produce `Ring`-style requests/responses.

  Examples of the request/response.

  ```
  GET using Basic authn
  req := {:accept :xml
          :basic-auth [\"user\" \"password\"]}
  resp := {:status 200
           :body \"<Hello :)>\"
           :headers {...}}
  ```

  ```
  GET using cookie
  req := {:accept :json
          :headers {:cookie cookie}}
  resp := {:status 200
           :body \"{\"Hello\": \":)\"}\"
           :headers {...}}
  ```

  ```clojure
  (:body (get \"https://httpbin.org/get\"))
  ```

  On HTTP error, thows ExceptionInfo with `:data` containing the full response.
  The response can be obtained with `(ex-data ex)`

  ```clojure
  (let [{:keys [status body]}
        (try
          (get \"https://httpbin.org/error\")
          (catch ExceptionInfo e (ex-data e)))
  ```
  "
  {:doc/format :markdown}
  (:refer-clojure :exclude [get])
  (:require
    [kvlt.core :as kvlt]
    [kvlt.chan :as kc]))

(defn- re-throw-ex-info
  [e]
  (throw (let [data (ex-data (.getCause e))]
           (ex-info (str "HTTP Error: " (:status data)) data))))

(defn request!
  "Synchronous request.  Throws `ExecutionInfo` on HTTP errors
   with `:data` as Ring-style response.
   To extract the response on error, catch `ExecutionInfo` and call
   `(ex-data e)`."
  {:doc/format :markdown}
  [meth url req]
  (try
    @(kvlt/request!
      (merge {:method (keyword meth) :url url} req))
    (catch java.util.concurrent.ExecutionException e (re-throw-ex-info e))))

(defn request-async!
  "Asynchronous request puts result (or error) onto a channel."
  [meth url {:keys [chan] :as req}]
  (kc/request! (merge {:method (keyword meth) :url url} req) {:chan chan}))

(defn get
  [url & [req]]
  (request! :get url req))

(defn get-async
  [url & [req]]
  (request-async! :get url req))

(defn put
  [url & [req]]
  (request! :put url req))

(defn post
  [url & [req]]
  (request! :post url req))

(defn delete
  [url & [req]]
  (request! :delete url req))
