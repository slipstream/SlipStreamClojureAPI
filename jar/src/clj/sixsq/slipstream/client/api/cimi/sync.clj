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
    (<!! (cimi/logout async-context)))
  (cloud-entry-point [_]
    (<!! (cimi/cloud-entry-point async-context)))
  (add [_ resource-type data]
    (<!! (cimi/add async-context resource-type data nil)))
  (add [_ resource-type data options]
    (<!! (cimi/add async-context resource-type data options)))
  (edit [_ url-or-id data]
    (<!! (cimi/edit async-context url-or-id data nil)))
  (edit [_ url-or-id data options]
    (<!! (cimi/edit async-context url-or-id data options)))
  (delete [_ url-or-id]
    (<!! (cimi/delete async-context url-or-id nil)))
  (delete [_ url-or-id options]
    (<!! (cimi/delete async-context url-or-id options)))
  (get [_ url-or-id]
    (<!! (cimi/get async-context url-or-id nil)))
  (get [_ url-or-id options]
    (<!! (cimi/get async-context url-or-id options)))
  (search [_ resource-type]
    (<!! (cimi/search async-context resource-type nil)))
  (search [_ resource-type options]
    (<!! (cimi/search async-context resource-type options))))

(defn instance
  "A convenience function for creating an instance that
   implements the CIMI protocol synchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (->cimi-sync (async/instance)))
  ([endpoint login-endpoint logout-endpoint]
   (->cimi-sync (async/instance endpoint login-endpoint logout-endpoint))))
