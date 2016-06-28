(ns sixsq.slipstream.client.api.authn
  "Provides utility functions to authenticate with a server.

  The library API functions in namespaces under **sixsq.slipstream.client.api.lib**
  use dynamic context defined in this namespace.  To bootstrap
  the API with the context use [[login!]].  Each time
  when called, it alters the root of the context with the new
  authentication token obtained after performing basic authn
  with the username/password provided to [[login!]].

  `(with-context new-context (api-function ...))` can be used to
  rebind the global context to the new provided one for the time of
  the invocation of a specific API function. The
  new context can, for example, be obtained by

      {:serviceurl \"https://my.slipstream\"
       :cookie (login username password)}

  Full example

      (require '[sixsq.slipstream.client.api.authn :as a])
      (require '[sixsq.slipstream.client.api.lib.app :as p])
      (require '[sixsq.slipstream.client.api.lib.run :as r])

      (def my-slipstream
        {:serviceurl \"https://my.slipstream\"
         :cookie (a/login username password)}

      (def run-uuid
        (last
          (clojure.string/split
            (with-context my-slipstream (p/deploy \"my/app\")))))

      (with-context my-slipstream
        (r/get-run-info run-uuid))"
  {:doc/format :markdown}
  (:require
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [sixsq.slipstream.client.api.authn-async :as aasync]
    [superstring.core :as s]
    [sixsq.slipstream.client.api.utils.utils :as u]
    [clojure.core.async :refer [<!!]]))

(def ^:const default-url "https://nuv.la")

(def ^:const login-resource "auth/login")

(defn to-login-url
  [service-url]
  (u/url-join [service-url login-resource]))

(def ^:const default-login-url (to-login-url default-url))

;;
;; Authentication context management for the namespaces in client.api.lib/*.
(def default-context {:serviceurl default-url})

(def ^:dynamic *context* default-context)

(defn select-context
  [context]
  (select-keys context [:serviceurl :username :password :cookie]))

;;
(defn set-context!
  "Should be called to provide service URL and connection token.
  The following map is expected

      {:serviceurl \"https://nuv.la\"
       :cookie     \"cookie\"}"
  {:doc/format :markdown}
  [context]
  (alter-var-root #'*context* (fn [_] (merge default-context (select-context context)))))

(defmacro with-context
  [context & body]
  `(binding [*context* (merge *context* ~context)] (do ~@body)))

(defn login
  "Synchronous login to the server.  Directly returns the access token.
   Not available in clojurescript."
  ([username password]
   (login username password default-login-url))
  ([username password login-url]
   (second (<!! (aasync/login-async username password login-url)))))

(defn endpoint-from-url
  [url]
  (->> (s/split url #"/")
       (take 3)
       (filter #(not (= % "")))
       (s/join "//")))

(defn login!
  "Synchronous login to the server.  Alters the root of the global dynamic
  authentication context used in the namespaces under
  **sixsq.slipstream.client.api.lib** to interact with the service.
  Returns the access token.
  Not available in clojurescript."
  {:doc/format :markdown}
  ([username password]
   (login! username password default-login-url))
  ([username password login-url]
   (let [token (login username password login-url)]
     (set-context! {:serviceurl (endpoint-from-url login-url)
                    :cookie     token})
     token)))

