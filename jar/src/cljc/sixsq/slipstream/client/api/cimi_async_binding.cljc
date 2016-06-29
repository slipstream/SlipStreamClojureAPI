(ns sixsq.slipstream.client.api.cimi-async-binding
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.cimi-scrud-protocol :refer [CimiScrudProtocol]]
    [sixsq.slipstream.client.api.cimi-async :as cimi]
    [sixsq.slipstream.client.api.authn-async :as authn]
    #?(:clj
    [clojure.core.async :refer [go <!]]
       :cljs [cljs.core.async :refer [<!]])))

(defn reify-cimi-async [cep-url login-url logout-url]
  (let [token-atom (atom nil)
        cep (cimi/cloud-entry-point cep-url)]
    (reify CimiScrudProtocol
      (add [_ resource-type data & options]
        (cimi/add @token-atom cep resource-type data))
      (edit [_ url-or-id data & options]
        (cimi/edit @token-atom cep url-or-id data))
      (delete [_ url-or-id & options]
        (cimi/delete @token-atom cep url-or-id))
      (get [_ url-or-id & options]
        (cimi/get @token-atom cep url-or-id))
      (search [_ resource-type & options]
        (cimi/search @token-atom cep resource-type))
      (cloud-entry-point [_]
        cep)
      (login [_ credentials]
        (go
          (let [[status token] (<! (authn/login-async (:username credentials) (:password credentials) login-url))]
            (if (= 200 status)
              (do
                (reset! token-atom token)
                status)
              (ex-info "login failed" {:status  status
                                       :message "login failed"})))))
      (logout [_]
        (go
          (let [[status token] (<! (authn/logout-async logout-url))]
            (if (= 200 status)
              (do
                (reset! token-atom token)
                status)
              (ex-info "logout failed" {:status  status
                                       :message "logout failed"}))))))))

