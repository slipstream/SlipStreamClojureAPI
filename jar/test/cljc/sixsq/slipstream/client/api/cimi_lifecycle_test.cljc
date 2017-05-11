(ns sixsq.slipstream.client.api.cimi-lifecycle-test
  "Runs lifecycle tests for CIMI resources against a live server. If no user
   credentials are provided, the lifecycle tests are 'no-ops'. To run these
   tests (typically from the REPL), do the following:

   ```clojure
   (require '[sixsq.slipstream.client.api.cimi-lifecycle-test :as t])
   (in-ns 'sixsq.slipstream.client.api.cimi-lifecycle-test)
   (def ^:dynamic *server-info* (set-server-info \"username\" \"password\" \"https://nuv.la/\"))
   (run-tests)
   ```

   **NOTE**: The value for \"my-server-root\" must end with a slash!
   "
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [kvlt.core]
    [sixsq.slipstream.client.api.cimi.async :as i]
    [sixsq.slipstream.client.api.cimi :as c]

    [clojure.core.async :refer #?(:clj  [chan <! >! go <!!]
                                  :cljs [chan <! >!])]
    [clojure.test :refer [deftest is are testing run-tests #?(:cljs async)]]))

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

(defn set-server-info [username password server-root]
  (when (and username password server-root)
    (let [endpoint (str server-root "api/cloud-entry-point")]
      {:username     username
       :password     password
       :cep-endpoint endpoint})))

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
       (let [{:keys [username password cep-endpoint]} *server-info*]

         ;; check configuration sanity
         (is username)
         (is password)
         (is cep-endpoint)

         ;; get the cloud entry point for server
         (let [context (i/instance cep-endpoint)
               cep (<! (c/cloud-entry-point context))]
           (is context)
           (is (map? cep))
           (is (:baseURI cep))
           (is (:events cep))

           ;; try logging in with false credentials
           (let [result (<! (c/login context {:href     "session-template/internal"
                                              :username "UNKNOWN"
                                              :password "USER"}))]
             (is (instance? #?(:clj Exception :cljs js/Error) result))
             (is (= 403 (:status (ex-data result))))
             (is (false? (<! (c/authenticated? context)))))

           ;; log into the server
           (let [{:keys [status]} (<! (c/login context {:href     "session-template/internal"
                                                        :username username
                                                        :password password}))]
             (is (= 201 status))
             (is (true? (<! (c/authenticated? context)))))

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
               (let [edit-resp (<! (c/edit context event-id read-event))]
                 (is (instance? #?(:clj Exception :cljs js/Error) edit-resp)))

               ;; delete the event and ensure that it is gone
               (let [delete-resp (<! (c/delete context event-id))]
                 (is (= 200 (:status delete-resp)))
                 (let [get-resp (<! (c/get context event-id))]
                   (is (instance? #?(:clj Exception :cljs js/Error) get-resp))
                   (is (= 404 (:status (ex-data get-resp))))))))

           ;; logout from the server
           (let [logout-response (<! (c/logout context))]
             (is (= 200 (:status logout-response)))
             (is (false? (<! (c/authenticated? context))))))))
     (if done (done)))))

(deftest check-event-lifecycle
  #?(:clj  (<!! (event-lifecycle))
     :cljs (async done (event-lifecycle done))))

