(ns sixsq.slipstream.client.api.cimi-lifecycle-test
  "Runs lifecycle tests for CIMI resources against a live server.  If no
   user credentials are provided, the lifecycle tests are 'no-ops'.  To run
   these tests (typically from the REPL), do the following:

   ```clojure
   (require '[sixsq.slipstream.client.api.cimi-lifecycle-test :as t])
   (t/set-server-info \"my-username\" \"my-password\" \"my-server-root\")
   (in-ns 'sixsq.slipstream.client.api.cimi-lifecycle-test)
   (run-tests)
   ```

   **NOTE**: The value for \"my-server-root\" must end with a slash!
   "
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [kvlt.core]
    [sixsq.slipstream.client.api.cimi.async :as i]
    [sixsq.slipstream.client.api.cimi :as c]

    #?(:clj
    [clojure.core.async :as a :refer [go chan <!! <! >!]]
       :cljs [cljs.core.async :refer [chan <! >!]])
    #?(:clj
    [clojure.test :refer [deftest is are testing run-tests]]
       :cljs [cljs.test :refer-macros [deftest is are testing run-tests async]])
    ))

;; silence the request/response debugging
(kvlt.core/quiet!)

(def example-event
  {:id          "123"
   :resourceURI "http://schemas.dmtf.org/cimi/2/Event"
   :created     "2015-01-16T08:20:00.0Z"
   :updated     "2015-01-16T08:20:00.0Z"

   :timestamp   "2015-01-10T08:20:00.0Z"
   :content     {:resource {:href "Run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
                 :state    "Started"}
   :type        "state"
   :severity    "medium"

   :acl         {:owner {:type      "USER"
                         :principal "loomis"}
                 :rules [{:right     "ALL"
                          :type      "USER"
                          :principal "loomis"}
                         {:right     "ALL"
                          :type      "ROLE"
                          :principal "ADMIN"}]}})

(def example-attr
  {:id            "123"
   :resourceURI   "http://sixsq.com/slipstream/1/ServiceAttribute"
   :created       "2015-01-16T08:20:00.0Z"
   :updated       "2015-01-16T08:20:00.0Z"

   :uri           "http://example.org/schema/1/object"
   :type          "type"
   :major-version 1
   :minor-version 2
   :patch-version 3
   :normative     false
   :en            {:name        "Object"
                   :description "One Object to rule them all!"}

   :acl           {:owner {:type      "USER"
                           :principal "loomis"}
                   :rules [{:right     "ALL"
                            :type      "USER"
                            :principal "loomis"}
                           {:right     "ALL"
                            :type      "ROLE"
                            :principal "ADMIN"}]}})

(defn set-server-info [username password server-root]
  (when (and username password server-root)
    (let [endpoint (str server-root "api/cloud-entry-point")
          login-endpoint (str server-root "auth/login")
          logout-endpoint (str server-root "auth/logout")]
      {:username        username
       :password        password
       :cep-endpoint    endpoint
       :login-endpoint  login-endpoint
       :logout-endpoint logout-endpoint})))

;; FIXME: Caution!  Do not commit credentials.
(def ^:dynamic *server-info* (set-server-info nil nil "https://nuv.la/"))

(defn strip-fields [m]
  (dissoc m :id :created :updated :acl :operations))

;;
;; CAUTION: If too many 'is' tests are added, the clojurescript compiler
;; may cause the stack to overflow.  This is apparently related to the issue
;; http://dev.clojure.org/jira/browse/ASYNC-40
;; The immediate solution is to eliminate some of the less useful tests.
;;

(defn event-lifecycle
  ([]
   (event-lifecycle nil))
  ([done]
   (go
     (if *server-info*
       (let [{:keys [username password cep-endpoint login-endpoint logout-endpoint]} *server-info*]

         ;; check configuration sanity
         (is username)
         (is password)
         (is cep-endpoint)
         (is login-endpoint)
         (is logout-endpoint)

         ;; get the cloud entry point for server
         (let [context (i/instance cep-endpoint login-endpoint logout-endpoint)
               cep (<! (c/cloud-entry-point context))]
           (is context)
           (is (map? cep))
           (is (:baseURI cep))
           (is (:events cep))

           ;; try logging in with false credentials
           (let [result (<! (c/login context {:username "UNKNOWN" :password "USER"}))]
             (is (= 401 (:login-status result))))

           ;; log into the server
           (let [{:keys [login-status]} (<! (c/login context {:username username :password password}))]
             (is (= 200 login-status)))

           ;; search for events (tests assume that real account with lots of events is used)
           (let [events (<! (c/search context "events" {:$first 10 :$last 20}))]
             (is (= 11 (count (:events events))))
             (is (pos? (:count events))))

           ;; add a new event resource
           (let [add-event-resp (<! (c/add context "events" example-event))]
             (is (= 201 (:status add-event-resp)))

             ;; read the event back; do a search for all events
             (let [event-id (:resource-id add-event-resp)
                   read-event (<! (c/get context event-id))
                   events (<! (c/search context "events"))]
               (is (= (strip-fields example-event) (strip-fields read-event)))
               (is (pos? (:count events)))

               ;; events cannot be edited
               (is (instance? #?(:clj Exception :cljs js/Error) (<! (c/edit context event-id read-event))))

               ;; delete the event and ensure that it is gone
               (let [delete-resp (<! (c/delete context event-id))]
                 (is (= 200 (:status delete-resp)))
                 (let [get-resp (<! (c/get context event-id))]
                   (is (instance? #?(:clj Exception :cljs js/Error) get-resp))))))

           ;; logout from the server
           (let [logout-status (<! (c/logout context))]
             (is (= 200 logout-status))))))
     (if done (done)))))

(deftest check-event-lifecycle
  #?(:clj  (<!! (event-lifecycle))
     :cljs (async done (event-lifecycle done))))

(defn attribute-lifecycle-async
  ([]
   (attribute-lifecycle-async nil))
  ([done]
   (go
     (if *server-info*
       (let [{:keys [username password cep-endpoint login-endpoint logout-endpoint]} *server-info*]

         ;; check configuration sanity
         (is username)
         (is password)
         (is cep-endpoint)
         (is login-endpoint)
         (is logout-endpoint)

         ;; get the cloud entry point for server
         (let [context (i/instance cep-endpoint login-endpoint logout-endpoint)
               cep (<! (c/cloud-entry-point context))]
           (is context)
           (is (map? cep))
           (is (:baseURI cep))
           (is (:serviceAttributes cep))

           ;; try logging in with false credentials
           (let [result (<! (c/login context {:username "UNKNOWN" :password "USER"}))]
             (is (= 401 (:login-status result))))

           ;; log into the server
           (let [{:keys [login-status]} (<! (c/login context {:username username :password password}))]
             (is (= 200 login-status)))

           ;; search for service attributes
           (let [attrs (<! (c/search context "serviceAttributes"))]
             (is (:count attrs)))

           ;; add a new service attribute
           (let [add-attr-resp (<! (c/add context "serviceAttributes" example-attr))]
             (is (= 201 (:status add-attr-resp)))

             (let [attr-id (:resource-id add-attr-resp)
                   read-attr (<! (c/get context attr-id))
                   attrs (<! (c/search context "serviceAttributes"))]
               (is (= (strip-fields example-attr) (strip-fields read-attr)))
               (is (pos? (:count attrs)))

               ;; update the service attribute
               (let [updated-attr (assoc read-attr :major-version 10)
                     edit-resp (<! (c/edit context attr-id updated-attr))
                     reread-attr (<! (c/get context attr-id))]
                 (is (= (strip-fields edit-resp) (strip-fields reread-attr)))
                 (is (= (strip-fields updated-attr) (strip-fields reread-attr))))

               ;; delete the service attribute and ensure that it is gone
               (let [delete-resp (<! (c/delete context attr-id))]
                 (is (= 200 (:status delete-resp)))
                 (let [get-resp (<! (c/get context attr-id))]
                   (is (instance? #?(:clj Exception :cljs js/Error) get-resp))))))

           ;; logout from the server
           (let [logout-status (<! (c/logout context))]
             (is (= 200 logout-status))))))
     (if done (done)))))

(deftest check-attribute-lifecycle-async
  #?(:clj  (<!! (attribute-lifecycle-async))
     :cljs (async done (attribute-lifecycle-async done))))
