# CIMI API

The SlipStream API is migrating from a custom REST API to the
[CIMI](https://www.dmtf.org/standards/cloud) standard from
[DMTF](http://dmtf.org).  All recent additions to the resource model
already follow the CIMI REST interface.

## Usage

The CIMI SCRUD operations are defined in the `cimi` protocol in the
`sixsq.slipstream.client.api.cimi` namespace.  To use those functions,
you must instantiate either a synchronous or asynchronous
implementation of that protocol.

```clojure
(ns my.namespace
 (:require
   [sixsq.slipstream.client.api.cimi :as cimi]
   [sixsq.slipstream.client.api.cimi.async :as async]
   [sixsq.slipstream.client.api.cimi.sync :as sync]))

;; Create an asynchronous client context.  Note that the
;; asynchronous client can be used from Clojure or ClojureScript.
(def async (async/create-cimi-async))

;; Returns a channel on which a document with directory of available
;; resource is pushed. User does not need to be authenticated.
(cimi/cloud-entry-point async)

;; Returns a channel with login status (HTTP code).
(cimi/login async {:username "user" :password "pass"})

;; Returns channel with document containing list of events.
(cimi/search async "events")

;; Same can be done with synchronous client, but in this case
;; all values are directly returned, rather than being pushed
;; onto a channel.
;; NOTE: Synchronous client is only available in Clojure.

(def sync (sync/create-cimi-sync))
(cimi/login sync {:username "user" :password "pass"})
(cimi/search sync "events")
```

When creating the client context without specific endpoints, then
the endpoints for the Nuvla service will be used.  See the API
documentation for details on specifying the endpoints.
