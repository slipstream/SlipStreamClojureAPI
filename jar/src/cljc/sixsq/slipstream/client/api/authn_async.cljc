(ns sixsq.slipstream.client.api.authn-async
  "Provides asynchronous utility functions to authenticate
   with a server. All of the functions return a channel that
   will contain the result of the call."
  {:doc/format :markdown}
  (:require
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [superstring.core :as s]
    [sixsq.slipstream.client.api.utils.utils :as u]
    #?(:clj
    [clojure.core.async :refer [chan go go-loop <! <!!]]
       :cljs [cljs.core.async :refer [chan <!]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]])))


(def ^:const default-url "https://nuv.la")

(def ^:const login-resource "auth/login")

(def ^:const logout-resource "auth/logout")

(defn to-login-url
  [service-url]
  (u/url-join [service-url login-resource]))

(defn to-logout-url
  [service-url]
  (u/url-join [service-url logout-resource]))

(def ^:const default-login-url (to-login-url default-url))

(def ^:const default-logout-url (to-logout-url default-url))

(defn- auth-resp-chan
  "Returns a channel with a transducer that will extract a 2-element
   vector from an authentication response containing the HTTP status
   code and the value of the Set-Cookie header."
  []
  (chan 1 (map (juxt :status #(get-in % [:headers :set-cookie])))))

(defn login-async
  "Uses the given username and password to log into the SlipStream
   server.  This returns a channel that will contain the results of
   the request.  The result is a two-element vector containing the
   HTTP status code and the returned authentication token (cookie).

   If `url` is not provided, then the default login URL will be used.

   **Note**: In some environments (notably web browsers) you may not
   have access to the returned cookie for security reasons.  In these
   cases the token will be nil, even if the request succeeded."
  {:doc/format :markdown}
  ([username password]
   (login-async username password default-login-url))
  ([username password url]
   (http/post url {:chan             (auth-resp-chan)
                   :follow-redirects false
                   :type             :application/x-www-form-urlencoded
                   :accept           :json
                   :form-params      {:username username
                                      :password password
                                      :authn-method "internal"}})))

(defn logout-async
  "Performs a logout of the user by sending an invalid, expired token.
   This returns a channel that will contain the results of
   the request.  The result is a two-element vector containing the
   HTTP status code and the returned authentication token (cookie).

   If `url` is not provided, then the default logout URL will be used.

   **Note**: In some environments (notably web browsers) you may not
   have access to the returned cookie for security reasons.  In these
   cases the token will be nil, even if the request succeeded."
  {:doc/format :markdown}
  ([]
   (logout-async default-logout-url))
  ([url]
   (http/post url {:chan             (auth-resp-chan)
                   :follow-redirects false
                   :type             :application/x-www-form-urlencoded
                   :accept           :json})))


