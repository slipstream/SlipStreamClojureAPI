(ns sixsq.slipstream.client.api.cimi.async
  "Defines the type that implements an asynchronous version of the CIMI
   protocol. All of the defined return a channel with the result."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.defaults :as defaults]
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.cimi.impl-async :as impl]
    [sixsq.slipstream.client.api.cimi.utils :as u]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.api.pricing :as pricing]
    [sixsq.slipstream.client.api.cimi.impl-pricing-async :as pi]
    [clojure.core.async :refer #?(:clj  [chan >! <! go]
                                  :cljs [chan >! <!])]))

(deftype cimi-async [endpoint state]
  cimi/cimi

  (login
    [this login-params]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/login @state login-params))]
        (u/update-state state :token token)
        response)))

  (logout
    [this]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/logout @state))]
        (u/update-state state :token token)
        response)))

  (authenticated?
    [this]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[session-id _] (<! (impl/current-session @state))]
        (not (nil? session-id)))))

  (cloud-entry-point
    [_]
    (go
      (or (:cep @state)
          (let [[cep token] (<! (impl/cloud-entry-point endpoint))]
            (u/update-state state :token token)
            (u/update-state state :cep cep)
            cep))))

  (add [this resource-type data]
    (cimi/add this resource-type data nil))
  (add [this resource-type data options]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/add @state resource-type data))]
        (u/update-state state :token token)
        response)))

  (edit [this url-or-id data]
    (cimi/edit this url-or-id data nil))
  (edit [this url-or-id data options]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/edit @state url-or-id data))]
        (u/update-state state :token token)
        response)))

  (delete [this url-or-id]
    (cimi/delete this url-or-id nil))
  (delete [this url-or-id options]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/delete @state url-or-id))]
        (u/update-state state :token token)
        response)))

  (get [this url-or-id]
    (cimi/get this url-or-id nil))
  (get [this url-or-id options]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/get @state url-or-id))]
        (u/update-state state :token token)
        response)))

  (search [this resource-type]
    (cimi/search this resource-type nil))
  (search [this resource-type options]
    (go
      (<! (cimi/cloud-entry-point this))
      (let [[response token] (<! (impl/search @state resource-type options))]
        (u/update-state state :token token)
        response)))

  pricing/pricing

  (place-and-rank [this module-uri connectors]
    (go
      (let [{:keys [baseURI]} (<! (cimi/cloud-entry-point this))
            endpoint (second (re-matches #"^(https?://[^/]+)/.*$" baseURI))]
        (<! (pi/place-and-rank @state endpoint module-uri connectors))))))

(defn instance
  "A convenience function for creating an instance that implements the CIMI
   protocol asynchronously. Use of this function is preferred to the raw
   constructor."
  ([]
   (instance defaults/cep-endpoint))
  ([cep-endpoint]
   (->cimi-async cep-endpoint (atom {}))))
