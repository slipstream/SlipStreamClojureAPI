(ns sixsq.slipstream.client.api.modules.impl-async
  "Provides methods for reading modules from the SlipStream server."
  (:refer-clojure :exclude [get])
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [sixsq.slipstream.client.api.modules.utils :as impl]
    [clojure.core.async :refer #?(:clj  [chan <! >! go]
                                  :cljs [chan <! >!])]))

(def default-modules-endpoint "https://nuv.la/module")     ;; must NOT end with a slash!

(def default-login-endpoint "https://nuv.la/auth/login")

(def default-logout-endpoint "https://nuv.la/auth/logout")

(defn- create-chan
  "Creates a channel that extracts the JSON body and then
   transforms the body into a clojure data structure with
   keywordized keys.  Any exceptions that occur in processing
   are pushed onto the channel."
  []
  (chan 1 (impl/body-as-json) identity))

(defn- create-string-chan
  "Creates a channel that extracts the as a string.  Any
   exceptions that occur in processing are pushed onto the
   channel."
  []
  (chan 1 (impl/body-as-string) identity))

(defn get
  "Reads the module identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token endpoint url-or-id]
  (let [url (impl/ensure-url endpoint url-or-id)
        opts (-> (impl/req-opts token)
                 (assoc :chan (create-chan)))]
    (http/get url opts)))

(defn get-string
  "Reads the module identified by the URL or resource id.  Returns
   the resource as an edn data structure in a channel."
  [token endpoint url-or-id]
  (let [url (impl/ensure-url endpoint url-or-id)
        opts (-> (impl/req-opts token)
                 (assoc :chan (create-string-chan)))]
    (http/get url opts)))


