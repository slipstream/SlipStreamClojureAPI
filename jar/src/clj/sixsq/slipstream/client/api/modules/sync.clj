(ns sixsq.slipstream.client.api.modules.sync
  "Defines the type that implements the modules protocol.  All of the
   defined methods are synchronous and return the results directly
   as a clojure data structure.

   **NOTE: The synchronous version of the API is only available
   in clojure.**"
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.modules.async :as async]
    [sixsq.slipstream.client.api.modules :as modules]
    [clojure.core.async :refer [<!!]]))

(deftype modules-sync [async-context]
  modules/modules
  (login [_ creds]
    (<!! (modules/login async-context creds)))
  (logout [_]
    (<!! (modules/logout async-context)))
  (get [_ url-or-id]
    (<!! (modules/get async-context url-or-id nil)))
  (get [_ url-or-id options]
    (<!! (modules/get async-context url-or-id options)))
  (get-children [_ url-or-id]
    (<!! (modules/get-children async-context url-or-id)))
  (get-children [_ url-or-id options]
    (<!! (modules/get-children async-context url-or-id options))))

(defn instance
  "A convenience function for creating an instance that
   implements the modules protocol synchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (->modules-sync (async/instance)))
  ([endpoint login-endpoint logout-endpoint]
   (->modules-sync (async/instance endpoint login-endpoint logout-endpoint))))
