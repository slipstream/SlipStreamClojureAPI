(ns sixsq.slipstream.client.api.cimi.sync
  "Defines the type that implements the CIMI protocol.  All of the
   defined methods are synchronous and return the results directly
   as a clojure data structure.

   **NOTE: The synchronous version of the API is only available
   in clojure.**"
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.cimi.async :as async]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [clojure.core.async :refer [<!!]]))

(deftype cimi-sync [async-context]
  cimi/cimi
  (login [_ creds]
    (<!! (cimi/login async-context creds)))
  (logout [_]
    nil)
  (cloud-entry-point [_]
    (<!! (cimi/cloud-entry-point async-context)))
  (add [_ resource-type data]
    (<!! (cimi/add async-context resource-type data)))
  (edit [_ url-or-id data]
    (<!! (cimi/edit async-context url-or-id data)))
  (delete [_ url-or-id]
    (<!! (cimi/delete async-context url-or-id)))
  (get [_ url-or-id]
    (<!! (cimi/get async-context url-or-id)))
  (search [_ resource-type]
    (<!! (cimi/search async-context resource-type))))

(defn create-cimi-sync
  "A convenience function for creating an object that
   implements the CIMI protocol synchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (->cimi-sync (async/create-cimi-async)))
  ([endpoint login-endpoint]
   (->cimi-sync (async/create-cimi-async endpoint login-endpoint))))
