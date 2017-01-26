(ns sixsq.slipstream.client.api.cimi.async
  "Defines the type that implements the CIMI protocol.  All of the
   defined methods are asynchronous and return a channel with the
   result."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.defaults :as defaults]
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.cimi.impl-async :as impl]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [clojure.core.async :refer #?(:clj  [chan >! <! go]
                                  :cljs [chan >! <!])]))

(deftype cimi-async [endpoint login-endpoint logout-endpoint state]
  cimi/cimi
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

  (cloud-entry-point [_]
    (go
      (or (:cep state)
          (let [cep (<! (impl/cloud-entry-point endpoint))]
            (swap! state assoc :cep cep)
            cep))))

  (add [this resource-type data]
    (cimi/add this resource-type data nil))
  (add [this resource-type data options]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/add token cep resource-type data)))))

  (edit [this url-or-id data]
    (cimi/edit this url-or-id data nil))
  (edit [this url-or-id data options]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/edit token cep url-or-id data)))))

  (delete [this url-or-id]
    (cimi/delete this url-or-id nil))
  (delete [this url-or-id options]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/delete token cep url-or-id)))))

  (get [this url-or-id]
    (cimi/get this url-or-id nil))
  (get [this url-or-id options]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/get token cep url-or-id)))))

  (search [this resource-type]
    (cimi/search this resource-type nil))
  (search [this resource-type options]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/search token cep resource-type options))))))

(defn instance
  "A convenience function for creating an instance that
   implements the CIMI protocol asynchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (instance defaults/cep-endpoint
             defaults/login-endpoint
             defaults/logout-endpoint))
  ([cep-endpoint login-endpoint logout-endpoint]
    (->cimi-async cep-endpoint login-endpoint logout-endpoint (atom {}))))
