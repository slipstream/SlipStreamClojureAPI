(ns sixsq.slipstream.client.api.utils.http-async
  "An asynchronous wrapper arount an HTTP library for simple CRUD actions.
   All functions return a core.async channel that will contain the HTTP
   response.

   The functions mirror those in the synchronous wrapper and generally take
   the same arguments.  An difference is that exceptions are not thrown on
   HTTP error response codes and are instead pushed into the channel."
  (:refer-clojure :exclude [get])
  (:require [kvlt.chan :as kvlt]))

(defn request!
  "Asynchronous request that returns a channel that will contain the
   result."
  [meth url req]
  (-> {:method (keyword meth) :url url}
      (merge req)
      (kvlt/request!)))

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


