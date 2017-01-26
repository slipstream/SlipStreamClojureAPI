(ns sixsq.slipstream.client.api.cimi.impl-async
  "Provides the core, low-level functions for SCRUD actions on
   CIMI resources. These are details of the implementation and
   are not a part of the public CIMI API."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.defaults :as defaults]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [sixsq.slipstream.client.api.utils.common :as cu]
    [sixsq.slipstream.client.api.utils.json :as json]
    [sixsq.slipstream.client.api.cimi.utils :as impl]
    [clojure.core.async :refer #?(:clj  [chan <! >! go]
                                  :cljs [chan <! >!])]))

(defn- create-chan
  "Creates a channel that extracts the JSON body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  []
  (chan 1 (json/body-as-json) identity))

(defn create-op-url-chan
  "Creates a channel that extracts the operations from a
   collection or resource."
  [op baseURI]
  (chan 1 (impl/extract-op-url op baseURI) identity))

(defn get-collection-op-url
  "Returns the URL for the given operation and collection
   within a channel."
  [token cep op collection-name]
  (let [baseURI (:baseURI cep)
        url (impl/get-collection-url cep collection-name)
        req (-> (cu/req-opts token)
                (assoc :chan (create-op-url-chan op baseURI)))]
    (http/get url req)))

(defn get-resource-op-url
  "Returns the URL for the given operation and collection
   within a channel."
  [token cep op url-or-id]
  (let [baseURI (:baseURI cep)
        url (cu/ensure-url baseURI url-or-id)
        req (-> (cu/req-opts token)
                (assoc :chan (create-op-url-chan op baseURI)))]
    (http/get url req)))

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  [token cep resource-type data]
  (go
    (if-let [add-url (<! (get-collection-op-url token cep "add" resource-type))]
      (let [opts (-> (cu/req-opts token (json/edn->json data))
                     (assoc :chan (create-chan)))]
        (<! (http/post add-url opts))))))

(defn edit
  "Updates an existing CIMI resource identified by the URL or resource
   id."
  [token cep url-or-id data]
  (let [c (create-chan)]
    (go
      (if-let [edit-url (<! (get-resource-op-url token cep "edit" url-or-id))]
        (let [opts (-> (cu/req-opts token (json/edn->json data))
                       (assoc :chan c))]
          (<! (http/put edit-url opts)))                    ;; FIXME: second :body deref needed?
        (let [exception (ex-info "unauthorized" {:status      403
                                                 :message     "unauthorized"
                                                 :resource-id url-or-id})]
          exception)))))

(defn delete
  "Deletes the CIMI resource identified by the URL or resource id from
   the server."
  [token cep url-or-id]
  (go
    (let [delete-url (<! (get-resource-op-url token cep "delete" url-or-id))]
      (let [opts (-> (cu/req-opts token)
                     (assoc :chan (create-chan)))]
        (<! (http/delete delete-url opts))))))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token cep url-or-id]
  (let [url (cu/ensure-url (:baseURI cep) url-or-id)
        opts (-> (cu/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources (in a channel). The list will be wrapped within
   an envelope containing the metadata of the collection and search."
  [token cep resource-type options]
  (let [url (impl/get-collection-url cep resource-type)
        opts (-> (cu/req-opts token)
                 (assoc :query-params options)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn cloud-entry-point
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns a channel which will contain the cloud
   entry point in edn format."
  ([]
   (cloud-entry-point defaults/cep-endpoint))
  ([endpoint]
   (let [opts (-> (cu/req-opts)
                  (assoc :chan (create-chan)))]
     (http/get endpoint opts))))

