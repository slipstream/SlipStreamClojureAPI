(ns sixsq.slipstream.client.api.modules
  (:refer-clojure :exclude [get]))

(defprotocol modules
  "Provides methods to retrieve SlipStream modules."

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
    "Reads the module identified by the URL or id and returns a
     data structure containing a full description of the module.")

  (get-children
    [this url-or-id]
    [this url-or-id options]
    "Reads the module identified by the URL or id and returns a
     list of the child identifiers. If the argument is nil, then
     the list of root modules is returned. If the module has no
     children, an empty list is returned.  If the module is not
     a project (i.e. can't have children), then nil is returned."))
