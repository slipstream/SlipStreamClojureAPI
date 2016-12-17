(ns sixsq.slipstream.client.api.runs.async
  "Defines the type that implements the runs protocol.  All of the
   defined methods are asynchronous and return a channel with the
   result."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.runs.impl-async :as impl]
    [sixsq.slipstream.client.api.runs :as runs]
    [clojure.core.async :refer #?(:clj  [chan >! <! go]
                                  :cljs [chan >! <!])]))

(deftype runs-async [endpoint login-endpoint logout-endpoint state]
  runs/runs
  (login [_ creds]
    (go
      (let [{:keys [username password]} creds
            resp (<! (authn/login-async-with-status username password login-endpoint))
            result (zipmap [:login-status :token] resp)]
        (swap! state merge result)
        result)))

  (logout [_]
    (go
      (let [status (<! (authn/logout-async-with-status logout-endpoint))]
        (if (= 200 status)
          (swap! state merge {:login-status nil :token nil}))
        status)))

  (get [this url-or-id]
    (runs/get this url-or-id nil))
  (get [this url-or-id options]
    (go
      (let [token (:token @state)]
        (<! (impl/get token endpoint url-or-id)))))

  (search [this]
    (runs/search this nil))
  (search [this options]
    (go
      (let [token (:token @state)]
        (<! (impl/search token endpoint options))))))

(defn instance
  "A convenience function for creating an instance that
   implements the runs protocol asynchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (instance impl/default-runs-endpoint
             impl/default-login-endpoint
             impl/default-logout-endpoint))
  ([cep-endpoint login-endpoint logout-endpoint]
    (->runs-async cep-endpoint login-endpoint logout-endpoint (atom {}))))
