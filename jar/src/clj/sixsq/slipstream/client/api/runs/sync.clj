(ns sixsq.slipstream.client.api.runs.sync
  "Defines the type that implements the runs protocol.  All of the
   defined methods are synchronous and return the results directly
   as a clojure data structure.

   **NOTE: The synchronous version of the API is only available
   in clojure.**"
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.runs.async :as async]
    [sixsq.slipstream.client.api.runs :as runs]
    [clojure.core.async :refer [<!!]]))

(deftype runs-sync [async-context]
  runs/runs
  (login [_ creds]
    (<!! (runs/login async-context creds)))
  (logout [_]
    (<!! (runs/logout async-context)))
  (get [_ url-or-id]
    (<!! (runs/get async-context url-or-id nil)))
  (get [_ url-or-id options]
    (<!! (runs/get async-context url-or-id options)))
  (search [_]
    (<!! (runs/search async-context nil)))
  (search [_ options]
    (<!! (runs/search async-context options))))

(defn instance
  "A convenience function for creating an instance that
   implements the runs protocol synchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (->runs-sync (async/instance)))
  ([endpoint login-endpoint logout-endpoint]
   (->runs-sync (async/instance endpoint login-endpoint logout-endpoint))))
