(ns sixsq.slipstream.client.api.cimi
  (:refer-clojure :exclude [get]))

(defprotocol cimi
  "All of the SCRUD (search, create, read, update, and delete) actions
   for CIMI resources.

   Note that the return types will depend on the concrete
   implementation.  For example, an asynchronous implementation will
   return channels from all of the functions."

  (login
    [this creds]
    "Uses the given credentials to log into the SlipStream server.
     It returns a tuple of the login status (HTTP status code) and
     a token (cookie).  The token may be nil even on a successful
     login depending on the concrete implementation.")

  (logout
    [this]
    "Removes any cached credentials and/or tokens. Subsequent
     requests will require the client to login again.")

  (cloud-entry-point
    [this]
    "Retrieves the cloud entry point.  The cloud entry point (CEP)
     acts as a directory of the available resources within the CIMI
     server. This function does not require authentication.  The
     result is returned in EDN format.")

  (add
    [this resource-type data]
    [this resource-type data options]
    "Creates a new CIMI resource of the given type. The data will be
     converted into a JSON string before being sent to the server. The
     data must match the schema of the resource type.")

  (edit
    [this url-or-id data]
    [this url-or-id data options]
    "Updates an existing CIMI resource identified by the URL or resource
     id.  The data must be the complete, updated data of the resource.
     Returns the updated resource in EDN format.")

  (delete
    [this url-or-id]
    [this url-or-id options]
    "Deletes the CIMI resource identified by the URL or resource id from
     the server.  Returns a map with the result.")

  (get
    [this url-or-id]
    [this url-or-id options]
    "Reads the CIMI resource identified by the URL or resource id.  Returns
     the resource as EDN data.")

  (search
    [this resource-type]
    [this resource-type options]
    "Search for CIMI resources of the given type, returning a list of the
     matching resources. The list will be wrapped within an envelope
     containing the metadata of the collection and search. The returned
     document is in EDN format."))
