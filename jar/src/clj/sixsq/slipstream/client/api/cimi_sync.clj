(ns sixsq.slipstream.client.api.cimi-sync
  "Provides the synchronous SCRUD actions on CIMI resources.
   All of the functions require an authentication token that can be
   obtained by logging into the server (see authn namespace).

   This implementation is simply a (clojure-only) convenience that
   wraps the asyncronous SCRUD functions."
  {:doc/format :markdown}
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.cimi-async :as cimi-async]
    [sixsq.slipstream.client.api.utils.error :as e]
    [clojure.core.async :as a :refer [<!!]]))

(defn add
  "Creates a new CIMI resource of the given type. The data will be
   converted into a JSON string before being sent to the server. The
   data must match the schema of the resource type."
  [token cep resource-type data]
  (e/throw-if-error (<!! (cimi-async/add token cep resource-type data))))

(defn edit
  "Updates an existing CIMI resource identified by the URL or resource
   id."
  [token cep url-or-id data]
  (e/throw-if-error (<!! (cimi-async/edit token cep url-or-id data))))

(defn delete
  "Deletes the CIMI resource identified by the URL or resource id from
   the server."
  [token cep url-or-id]
  (e/throw-if-error (<!! (cimi-async/delete token cep url-or-id))))

(defn get
  "Reads the CIMI resource identified by the URL or resource id.  Returns
   the resource as an edn data structure."
  {:doc/format :markdown}
  [token cep url-or-id]
  (e/throw-if-error (<!! (cimi-async/get token cep url-or-id))))

(defn search
  "Search for CIMI resources of the given type, returning a list of the
   matching resources. The list will be wrapped within an envelope containing
   the metadata of the collection and search."
  {:doc/format :markdown}
  [token cep resource-type]
  (e/throw-if-error (<!! (cimi-async/search token cep resource-type))))

(defn cloud-entry-point
  "Retrieves the cloud entry point from the given endpoint.  The cloud
   entry point acts as a directory of the available resources within
   the CIMI server. This returns the cloud entry point in edn format."
  {:doc/format :markdown}
  ([]
   (e/throw-if-error (<!! (cimi-async/cloud-entry-point))))
  ([endpoint]
   (e/throw-if-error (<!! (cimi-async/cloud-entry-point endpoint)))))
