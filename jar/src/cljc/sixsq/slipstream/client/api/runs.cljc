(ns sixsq.slipstream.client.api.runs
  (:refer-clojure :exclude [get]))

(defprotocol runs
  "Methods to get and search for runs.

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

  (get
    [this url-or-id]
    [this url-or-id options]
    "Reads the run identified by the URL or resource id.  Returns
     the resource as EDN data.")

  (search
    [this]
    [this options]
    "Search for runs of the given type, returning a list of the
     matching runs. Supported options are :cloud, :activeOnly,
     :offset, and :limit. The returned document is in EDN format."))
