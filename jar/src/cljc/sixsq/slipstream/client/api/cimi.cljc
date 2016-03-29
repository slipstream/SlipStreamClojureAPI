(ns sixsq.slipstream.client.api.cimi
  "Provides the core functions for SCRUD actions on CIMI resources.
   All of the functions require an authentication token that can be
   obtained by logging into the server (see authn namespace). "
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.utils.http :as http]
    [sixsq.slipstream.client.api.impl.cimi :as impl]))

(def default-endpoint "https://nuv.la/api/cloud-entry-point")

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  [token cep resource-type data]
  (if-let [add-url (:add (impl/get-collection-operations token cep resource-type))]
    (let [req (impl/req-opts token (impl/edn->json data))]
      (-> add-url
          (http/post req)
          :body
          impl/parse-json))))

(defn edit
  "Updates an existing CIMI resource identified by the URL or resource
   id."
  [token cep url-or-id data]
  (if-let [edit-url (:edit (impl/get-resource-operations token cep url-or-id))]
    (let [req (impl/req-opts token (impl/edn->json data))]
      (-> edit-url
          (http/put req)
          :body
          impl/parse-json
          :body))))                                         ;; FIXME: why is this needed?

(defn delete
  "Deletes the CIMI resource identified by the URL or resource id from
   the server."
  [token cep url-or-id]
  (let [delete-url (:delete (impl/get-resource-operations token cep url-or-id))]
    (let [req (impl/req-opts token)]
      (http/delete delete-url req))))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure."
  [token cep url-or-id]
  (-> (impl/ensure-url cep url-or-id)
      (http/get (impl/req-opts token))
      :body
      impl/parse-json))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources. The list will be wrapped within an envelope containing
   the metadata of the collection and search."
  [token cep resource-type]
  (-> (impl/get-collection-url cep resource-type)
      (http/get (impl/req-opts token))
      :body
      impl/parse-json))

(defn cloud-entry-point
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns the cloud entry point in edn format.

   FIXME: This resource should not require authentication, but it
   currently does.  When this is corrected, the token can be removed
   from the function signature."
  ([token]
   (cloud-entry-point token default-endpoint))
  ([token endpoint]
   (-> endpoint
       (http/get (impl/req-opts token))
       :body
       impl/parse-json)))
