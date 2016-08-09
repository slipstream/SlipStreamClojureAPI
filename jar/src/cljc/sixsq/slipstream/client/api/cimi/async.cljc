(ns sixsq.slipstream.client.api.cimi.async
  "Defines the type that implements the CIMI protocol.  All of the
   defined methods are asynchronous and return a channel with the
   result."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.cimi.impl-async :as impl]
    [sixsq.slipstream.client.api.cimi :as cimi]
    #?(:clj  [clojure.core.async :refer [go chan >! <!]]
       :cljs [cljs.core.async :refer [chan >! <!]])))

(deftype cimi-async [endpoint login-endpoint state]
  cimi/cimi
  (login [_ creds]
    (go
      (let [{:keys [username password]} creds
            resp (<! (authn/login-async-with-status username password login-endpoint))
            result (zipmap [:login-status :token] resp)]
        (swap! state merge result)
        result)))
  (logout [_]
    nil)
  (cloud-entry-point [_]
    (go
      (or (:cep state)
          (let [cep (<! (impl/cloud-entry-point endpoint))]
            (swap! state assoc :cep cep)
            cep))))
  (add [this resource-type data]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/add token cep resource-type data)))))
  (edit [this url-or-id data]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/edit token cep url-or-id data)))))
  (delete [this url-or-id]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/delete token cep url-or-id)))))
  (get [this url-or-id]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/get token cep url-or-id)))))
  (search [this resource-type]
    (go
      (let [cep (<! (cimi/cloud-entry-point this))
            token (:token @state)]
        (<! (impl/search token cep resource-type))))))

(defn create-cimi-async
  "A convenience function for creating an object that
   implements the CIMI protocol asynchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (create-cimi-async impl/default-cep-endpoint impl/default-login-endpoint))
  ([cep-endpoint login-endpoint]
    (->cimi-async cep-endpoint login-endpoint (atom {}))))
