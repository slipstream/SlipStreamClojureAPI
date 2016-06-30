(ns sixsq.slipstream.client.api.cimi-async-binding
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.cimi-scrud-protocol :refer [CimiScrudProtocol]]
    [sixsq.slipstream.client.api.cimi-async :as cimi]
    [sixsq.slipstream.client.api.authn-async :as authn]
    #?(:clj
    [clojure.core.async :refer [go chan <! >!]]
       :cljs [cljs.core.async :refer [chan <! >!]])))

(defn- handle-authn-resp
  "Resets the token, if necessary, and returns a channel with the status of the call
   or an exception with the status and error message."
  [token-atom c]
  (go
    (let [[status token] (<! c)]
      (if (= 200 status)
        (do
          (reset! token-atom token)
          status)
        (ex-info "login/logout failed" {:status  status
                                        :message "login/logout failed"})))))

(defn reify-cimi-async
  "Creates an object that implements the CimiScrudProtocol. This function returns
   a channel that will contain either the object or an exception.  Note that the
   SCRUD actions will not work until the `login` method has been called and
   returns successfully."
  {:doc/format :markdown}
  [cep-url login-url logout-url]
  (let [token-atom (atom nil)
        cep {}  #_(<! (cimi/cloud-entry-point cep-url))]
    (go
      (reify CimiScrudProtocol
        (add [_ resource-type data]
          (cimi/add @token-atom cep resource-type data))
        (edit [_ url-or-id data]
          (cimi/edit @token-atom cep url-or-id data))
        (delete [_ url-or-id]
          (cimi/delete @token-atom cep url-or-id))
        (get [_ url-or-id]
          (cimi/get @token-atom cep url-or-id))
        (search [_ resource-type]
          (cimi/search @token-atom cep resource-type))
        (cloud-entry-point [_]
          (>! (chan) cep))
        (login [_ credentials]
          (let [[status token] (<! (authn/login-async (:username credentials) (:password credentials) login-url))]
            (if (= 200 status)
              (do
                (reset! token-atom token)
                status)
              (ex-info "login/logout failed" {:status  status
                                              :message "login/logout failed"}))))
        (logout [_]
          (let [[status token] (<! (authn/logout-async logout-url))]
            (if (= 200 status)
              (do
                (reset! token-atom token)
                status)
              (ex-info "login/logout failed" {:status  status
                                              :message "login/logout failed"}))))))))
