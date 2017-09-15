(ns sixsq.slipstream.client.api.runs)

(defprotocol runs
  "Functions to get and search for runs.

   This protocol targets the legacy SlipStream interface and provides
   limited functionality.  The deployment resources are being migrated
   to the CIMI protocol and this protocol will disappear once that
   migration is complete.

   Note that the return types will depend on the concrete
   implementation.  For example, an asynchronous implementation will
   return channels from all of the functions."

  (get-run
    [this url-or-id]
    [this url-or-id options]
    "Reads the run identified by the URL or resource id.  Returns
     the resource as EDN data.")

  (search-runs
    [this]
    [this options]
    "Search for runs of the given type, returning a list of the
     matching runs. Supported options are :cloud, :activeOnly,
     :offset, and :limit. The returned document is in EDN format."))
