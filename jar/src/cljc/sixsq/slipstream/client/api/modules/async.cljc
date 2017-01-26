(ns sixsq.slipstream.client.api.modules.async
  "Defines the type that implements the modules protocol.  All of the
   defined methods are asynchronous and return a channel with the
   result."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.defaults :as defaults]
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.modules.impl-async :as impl]
    [sixsq.slipstream.client.api.modules.utils :as utils]
    [sixsq.slipstream.client.api.modules :as modules]
    [clojure.core.async :refer #?(:clj  [chan >! <! go]
                                  :cljs [chan >! <!])]))

(deftype modules-async [endpoint login-endpoint logout-endpoint state]
  modules/modules
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
    (modules/get this url-or-id nil))
  (get [this url-or-id options]
    (go
      (let [token (:token @state)]
        (<! (impl/get token endpoint url-or-id)))))

  (get-children [this url-or-id]
    (modules/get-children this url-or-id nil))
  (get-children [this url-or-id options]
    (go
      (let [token (:token @state)]
        (if url-or-id
          (let [module (<! (impl/get token endpoint url-or-id))]
            (utils/extract-children module))
          (let [xml (<! (impl/get-string token endpoint nil))]
            (utils/extract-xml-children xml)))))))

(defn instance
  "A convenience function for creating an instance that
   implements the modules protocol asynchronously.  Use of
   this function is preferred to the raw constructor."
  ([]
   (instance defaults/modules-endpoint
             defaults/login-endpoint
             defaults/logout-endpoint))
  ([modules-endpoint login-endpoint logout-endpoint]
    (->modules-async modules-endpoint login-endpoint logout-endpoint (atom {}))))
