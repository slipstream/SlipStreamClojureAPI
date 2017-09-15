(ns ^{:no-doc true} sixsq.slipstream.client.impl.modules-async
  "Provides methods for reading modules from the SlipStream server."
  (:require
    [sixsq.slipstream.client.impl.utils.error :as e]
    [sixsq.slipstream.client.impl.utils.http-async :as http]
    [sixsq.slipstream.client.impl.utils.common :as cu]
    [sixsq.slipstream.client.impl.utils.json :as json]
    [sixsq.slipstream.client.impl.utils.modules :as impl]
    [clojure.core.async :refer #?(:clj  [chan]
                                  :cljs [chan])]))

(defn- create-chan
  "Creates a channel that extracts the JSON body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  []
  (chan 1 (json/body-as-json) identity))

(defn- create-string-chan
  "Creates a channel that extracts the as a string.  Any
   exceptions that occur in processing are pushed onto the
   channel."
  []
  (chan 1 (cu/body-as-string) identity))

(defn get-module
  "Reads the module identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token endpoint url-or-id]
  (let [url (cu/ensure-url-slash endpoint url-or-id)
        opts (-> (cu/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn get-module-string
  "Reads the module identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token endpoint url-or-id]
  (let [url (cu/ensure-url-slash endpoint url-or-id)
        opts (-> (cu/req-opts token)
                 (assoc :chan (create-string-chan)))]
    (http/get url opts)))


