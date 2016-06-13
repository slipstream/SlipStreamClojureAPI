(ns sixsq.slipstream.client.api.utils.http-async
  "Asynchronous wrapper around standard HTTP calls to provide a uniform interface.

  All actions accept requests in Ring-like format and return a channel.  All results
  and errors are placed on the returned channel."
  {:doc/format :markdown}
  (:refer-clojure :exclude [get])
  (:require
    [kvlt.chan :as kc]))

(defn- request-async!
  [meth url {:keys [chan] :as req}]
  (kc/request! (merge {:method (keyword meth) :url url} req) {:chan chan}))

(defn get
  [url & [req]]
  (request-async! :get url req))

(defn put
  [url & [req]]
  (request-async! :put url req))

(defn post
  [url & [req]]
  (request-async! :post url req))

(defn delete
  [url & [req]]
  (request-async! :delete url req))
