(ns com.sixsq.slipstream.clj-client.authn
  "Provides a utility to log into the SlipStream server and to
   recover an access token.  No logout function is provided as
   the access can be removed by just destroying the token."
  (:require
    [com.sixsq.slipstream.clj-client.lib.http-async-impl :as http]
    #?(:clj
    [clojure.core.async :refer [go go-loop <! <!!]]
       :cljs [cljs.core.async :refer [<!]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]])))

(def ^:const default-login-url "https://nuv.la/login")

(defn login-async
  "Uses the given username and password to log into the SlipStream
   server.  Returns the token (cookie) to be used in subsequent
   requests.  If called without an explicit login-url, then the
   default on Nuvla is used.

   This method returns a channel that will contain the result.

   **FIXME**: Ideally the login-url should be discovered from the cloud
   entry point. This requires that the cloud entry point be accessible
   without credentials."
  ([username password]
   (login-async username password default-login-url))
  ([username password login-url]
   (let [data (str "authn-method=internal&username=" username "&password=" password)]
     (go
       (let [result (<! (http/post login-url {:follow-redirects false
                                              :body             data}))]
         (-> result :headers :set-cookie))))))

#?(:clj
   (defn login
     "Synchronous login to the server.  Directly returns the access token.
      Not available in clojurescript."
     ([username password]
      (login username password default-login-url))
     ([username password login-url]
      (<!! (login-async username password login-url)))))
