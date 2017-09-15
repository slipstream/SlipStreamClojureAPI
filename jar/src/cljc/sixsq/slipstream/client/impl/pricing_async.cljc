(ns ^{:no-doc true} sixsq.slipstream.client.impl.pricing-async
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.impl.utils.error :as e]
    [sixsq.slipstream.client.impl.utils.http-async :as http]
    [sixsq.slipstream.client.impl.utils.common :as cu]
    [sixsq.slipstream.client.impl.utils.json :as json]
    [sixsq.slipstream.client.impl.utils.cimi :as u]
    [sixsq.slipstream.client.api.deprecated-authn :as authn]
    [cemerick.url :as url]
    [clojure.set :as set]
    [clojure.core.async :refer #?(:clj  [chan <! >! go]
                                  :cljs [chan <! >!])]))

(defn json-body
  [{:keys [body] :as response}]
  (json/json->edn body))

(defn json-body-xducer
  []
  (comp
    (map e/throw-if-error)
    (map json-body)))

(defn- create-chan
  []
  (chan 1 (json-body-xducer) identity))

(defn place-and-rank
  [{:keys [token] :as state} endpoint module-uri connectors]
  (go
    (let [req {:moduleUri      module-uri
               :userConnectors connectors}
          opts (-> (cu/req-opts token (json/edn->json req))
                   (assoc :chan (create-chan)))
          module-info (<! (http/put (str endpoint "/ui/placement") opts))]
      (if-not (e/error? module-info)
        (let [opts (-> (cu/req-opts token (json/edn->json module-info))
                       (assoc :chan (create-chan)))
              pricing-info (<! (http/put (str endpoint "/filter-rank") opts))]
          pricing-info)
        module-info))))
