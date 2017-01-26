(ns sixsq.slipstream.client.api.runs.impl-async
  "Provides the core, low-level functions for SCRUD actions on
   run resources. These are details of the implementation and
   are not a part of the public API."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.defaults :as defaults]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [sixsq.slipstream.client.api.utils.common :as cu]
    [sixsq.slipstream.client.api.utils.json :as json]
    [sixsq.slipstream.client.api.utils.xml :as xml]
    [clojure.core.async :refer #?(:clj  [chan <! >! go]
                                  :cljs [chan <! >!])]))

(defn- create-chan
  "Creates a channel that extracts the JSON body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  []
  (chan 1 (json/body-as-json) identity))

(defn- create-xml-chan
  "Creates a channel that extracts the XML body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  []
  (chan 1 (xml/body-as-xml) identity))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token endpoint url-or-id]
  (let [url (cu/ensure-url-slash endpoint url-or-id)
        opts (-> (cu/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources (in a channel). The list will be wrapped within
   an envelope containing the metadata of the collection and search."
  [token endpoint options]
  (let [opts (-> (cu/req-opts token)
                 (assoc :query-params options)
                 (assoc :chan (create-xml-chan)))]
    (http/get endpoint opts)))

(defn cloud-entry-point
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns a channel which will contain the cloud
   entry point in edn format."
  ([]
   (cloud-entry-point defaults/runs-endpoint))
  ([endpoint]
   (let [opts (-> (cu/req-opts)
                  (assoc :chan (create-chan)))]
     (http/get endpoint opts))))

