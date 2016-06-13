(ns sixsq.slipstream.client.api.cimi-async
  "Provides the core functions for SCRUD actions on CIMI resources.
   All of the functions require an authentication token that can be
   obtained by logging into the server (see authn namespace). "
  {:doc/format :markdown}
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [sixsq.slipstream.client.api.impl.cimi :as impl]
    #?(:clj  [clojure.core.async :refer [chan]]
       :cljs [cljs.core.async :refer [chan]])))

(def default-endpoint "https://nuv.la/api/cloud-entry-point")

(defn- create-chan
  "Creates a channel that extracts the JSON body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  {:doc/format :markdown}
  []
  (chan 1 (impl/body-as-json) identity))

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  {:doc/format :markdown}
  [token cep resource-type data]
  (if-let [add-url (:add (impl/get-collection-operations token cep resource-type))]
    (let [opts (-> (impl/req-opts token (impl/edn->json data))
                   (assoc :chan (create-chan)))]
      (http/post add-url opts))))

(defn edit
  "Updates an existing CIMI resource identified by the URL or resource
   id."
  {:doc/format :markdown}
  [token cep url-or-id data]
  (if-let [edit-url (:edit (impl/get-resource-operations token cep url-or-id))]
    (let [opts (-> (impl/req-opts token (impl/edn->json data))
                   (assoc :chan (create-chan)))]
      (http/put edit-url opts))))                                         ;; FIXME: second :body selection needed?

(defn delete
  "Deletes the CIMI resource identified by the URL or resource id from
   the server."
  {:doc/format :markdown}
  [token cep url-or-id]
  (let [delete-url (:delete (impl/get-resource-operations token cep url-or-id))]
    (let [opts (-> (impl/req-opts token)
                   (assoc :chan (create-chan)))]
      (http/delete delete-url opts))))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  {:doc/format :markdown}
  [token cep url-or-id]
  (let [url (impl/ensure-url cep url-or-id)
        opts (-> (impl/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources (in a channel). The list will be wrapped within
   an envelope containing the metadata of the collection and search."
  {:doc/format :markdown}
  [token cep resource-type]
  (let [url (impl/get-collection-url cep resource-type)
        opts (-> (impl/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn cloud-entry-point
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns a channel which will contain the cloud
   entry point in edn format."
  {:doc/format :markdown}
  ([]
   (cloud-entry-point default-endpoint))
  ([endpoint]
   (let [opts (-> (impl/req-opts)
                  (assoc :chan (create-chan)))]
     (http/get endpoint opts))))
