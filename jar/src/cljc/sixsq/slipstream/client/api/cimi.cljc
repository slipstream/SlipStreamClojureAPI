(ns sixsq.slipstream.client.api.cimi
  "Provides the core functions for SCRUD actions on CIMI resources.
   All of the functions require an authentication token that can be
   obtained by logging into the server (see authn namespace). "
  {:doc/format :markdown}
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.utils.http :as http]
    [sixsq.slipstream.client.api.impl.cimi :as impl]
    #?(:clj [clojure.core.async :as a :refer [chan <!!]]
       :cljs [cljs.core.async :refer [chan]])))

(def default-endpoint "https://nuv.la/api/cloud-entry-point")

(defn body-as-json
  "transducer that extracts the body of a response and parses
   the result as JSON"
  []
  (comp
    (map :body)
    (map impl/parse-json)))

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  {:doc/format :markdown}
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
  {:doc/format :markdown}
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
  {:doc/format :markdown}
  [token cep url-or-id]
  (let [delete-url (:delete (impl/get-resource-operations token cep url-or-id))]
    (let [req (impl/req-opts token)]
      (http/delete delete-url req))))

(defn get-async
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  {:doc/format :markdown}
  [token cep url-or-id]
  (let [url (impl/ensure-url cep url-or-id)
        c (chan 1 body-as-json)
        opts (-> (impl/req-opts token)
                 (assoc :chan c))]
    (http/get-async url opts)))

#?(:clj
   (defn get
     "Reads the CIMI resource identified by the URL or resource id.  Returns
      the resource as an edn data structure."
     [token cep url-or-id]
     (<!! (get-async token cep url-or-id))))

(defn search-async
  "Search for CIMI resources of the given type, returning a list of the
   matching resources (in a channel). The list will be wrapped within
   an envelope containing the metadata of the collection and search."
  {:doc/format :markdown}
  [token cep resource-type]
  (let [url (impl/get-collection-url cep resource-type)
        c (chan 1 body-as-json)
        opts (-> (impl/req-opts token)
                 (assoc :chan c))]
    (http/get-async url opts)))

#?(:clj
   (defn search
     "Search for CIMI resources of the given type, returning a list of the
      matching resources. The list will be wrapped within an envelope containing
      the metadata of the collection and search."
          [token cep resource-type]
          (<!! (search-async token cep resource-type))))

(defn cloud-entry-point-async
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns a channel which will contain the cloud
   entry point in edn format."
  ([]
   (cloud-entry-point-async default-endpoint))
  ([endpoint]
   (let [c (chan 1 body-as-json)
         opts (-> (impl/req-opts)
                  (assoc :chan c))]
     (http/get-async endpoint opts))))

#?(:clj
   (defn cloud-entry-point
          "Retrieves the cloud entry point from the given endpoint.  The cloud
           entry point acts as a directory of the available resources within
           the CIMI server. This returns the cloud entry point in edn format."
          ([]
           (<!! (cloud-entry-point-async)))
          ([endpoint]
           (<!! (cloud-entry-point-async endpoint)))))
