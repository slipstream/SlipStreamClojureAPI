(ns sixsq.slipstream.client.api.authn
  "Provides a utility to log into the SlipStream server and to
  recover an access token.  No logout function is provided as
  the access can be removed by just destroying the token.

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
        (r/get-run-info run-uuid))

  To contact server in an insecure mode (i.e. w/o checking the authenticity of
  the remote server) before calling login functions, re-set the root of the
  authentication context with

      (require '[sixsq.slipstream.client.api.authn :as a])

      (a/set-context! {:insecure? true})
      (a/login username password)

  or wrap the API call for the local rebinding of the authentication context as 
  follows

      (a/with-context {:insecure? true}
        (a/login! username password))
  "
  {:doc/format :markdown}
  (:require
    [sixsq.slipstream.client.api.utils.http-async :as http]
    [superstring.core :as s]
    [sixsq.slipstream.client.api.utils.utils :as u]
    #?(:clj
    [clojure.core.async :refer [go go-loop <! <!!]]
       :cljs [cljs.core.async :refer [<!]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]])))


(def ^:const default-url "https://nuv.la")

(def ^:const login-resource "login")

(defn to-login-url
  [service-url]
  (u/url-join [service-url login-resource]))

(def ^:const default-login-url (to-login-url default-url))

;;
;; Authentication context management for the namespaces in client.api.lib/*.
(def default-context {:serviceurl default-url
                      :insecure? false})

(def ^:dynamic *context* default-context)

(defn select-context
  [context]
  (select-keys context [:serviceurl :username :password :cookie :insecure?]))

;;
#?(:clj
   (defn set-context!
     "Should be called to provide service URL and connection token.
     The following map is expected

         {:serviceurl \"https://nuv.la\"
          :cookie     \"cookie\"}"
     {:doc/format :markdown}
     [context]
     (alter-var-root #'*context* (fn [_] (merge default-context (select-context context))))))

(defmacro with-context
  [context & body]
  `(binding [*context* (merge *context* ~context)] (do ~@body)))


(defn login-async
  "Uses the given username and password to log into the SlipStream
   server.  Returns the token (cookie) to be used in subsequent
   requests.  If called without an explicit login-url, then the
   default on Nuvla is used.

   This method returns a channel that will contain the result.

   **FIXME**: Ideally the login-url should be discovered from the cloud
   entry point. This requires that the cloud entry point be accessible
   without credentials."
  {:doc/format :markdown}
  ([username password]
   (login-async username password default-login-url))
  ([username password login-url]
   (let [data (str "authn-method=internal&username=" username "&password=" password)]
     (go

       (let [result (<! (http/post login-url {:content-type     "application/x-www-form-urlencoded"
                                              :follow-redirects false
                                              :body             data
                                              :insecure?        (:insecure? *context*)}))]
         (-> result :headers :set-cookie))))))

#?(:clj
   (defn login
     "Synchronous login to the server.  Directly returns the access token.
      Not available in clojurescript."
     ([username password]
      (login username password default-login-url))
     ([username password login-url]
      (<!! (login-async username password login-url)))))

#?(:clj
   (defn endpoint-from-url
     [url]
     (->> (s/split url #"/")
          (take 3)
          (filter #(not (= % "")))
          (s/join "//"))))

#?(:clj
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
        (set-context! (merge *context*
                             {:serviceurl (endpoint-from-url login-url)
                              :cookie     token}))
        token))))

