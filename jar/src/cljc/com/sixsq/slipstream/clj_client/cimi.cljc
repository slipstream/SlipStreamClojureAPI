(ns com.sixsq.slipstream.clj-client.cimi
  "Provides the core functions for SCRUD actions on CIMI resources.
   All of the functions require an authentication token that can be
   obtained by logging into the server (see authn namespace). "
  (:refer-clojure :exclude [get])
  (:require
    [com.sixsq.slipstream.clj-client.lib.http-impl :as http]
    [com.sixsq.slipstream.clj-client.lib.utils.cimi-utils :as u]))

(def default-endpoint "https://nuv.la/api/cloud-entry-point")

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  [token cep resource-type data]
  (if-let [add-url (:add (u/get-collection-operations token cep resource-type))]
    (let [req (u/req-opts token (u/edn->json data))]
      (-> add-url
          (http/post req)
          :body
          u/parse-json))))

(defn edit
  "Updates an existing CIMI resource identified by the URL or resource
   id."
  [token cep url-or-id data]
  (if-let [edit-url (:edit (u/get-resource-operations token cep url-or-id))]
    (let [req (u/req-opts token (u/edn->json data))]
      (-> edit-url
          (http/put req)
          :body
          u/parse-json
          :body))))                                         ;; FIXME: why is this needed?

(defn delete
  "Deletes the CIMI resource identified by the URL or resource id from
   the server."
  [token cep url-or-id]
  (let [delete-url (:delete (u/get-resource-operations token cep url-or-id))]
    (let [req (u/req-opts token)]
      (http/delete delete-url req))))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure."
  [token cep url-or-id]
  (-> (u/ensure-url cep url-or-id)
      (http/get (u/req-opts token))
      :body
      u/parse-json))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources. The list will be wrapped within an envelope containing
   the metadata of the collection and search."
  [token cep resource-type]
  (-> (u/get-collection-url cep resource-type)
      (http/get (u/req-opts token))
      :body
      u/parse-json))

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
       (http/get (u/req-opts token))
       :body
       u/parse-json)))
