(ns sixsq.slipstream.client.api.cimi-scrud-protocol
  (:refer-clojure :exclude [get]))

(defprotocol CimiScrudProtocol
  "Provides the core functions for SCRUD actions on CIMI resources.

   Note that the return values of the functions may vary between
   concrete implementations of the protocol.  Specifically,
   asynchronous implementations always return channels where
   synchronous implementations will return the results directly."
  (add [_ resource-type data])
  (edit [_ url-or-id data])
  (delete [_ url-or-id])
  (get [_ url-or-id])
  (search [_ resource-type])
  (cloud-entry-point [_])
  (login [_ credentials])
  (logout [_]))


