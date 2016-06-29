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
   These tests make an assumption about the location of the cloud entry
   point URL and the login URL relative to the value given for
   \"my-server-root\".  In the future, all of the required URLs will be
   available from the cloud entry point.

   **NOTE**: The value for \"my-server-root\" must end with a slash!
   "
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [sixsq.slipstream.client.api.cimi-async :as t]
    [sixsq.slipstream.client.api.authn-async :as authn]
    [superstring.core :as s]
    [cljs.core.async :refer [put! chan <!]]
    [cljs.test :refer-macros [deftest is testing run-tests async]]))

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
          login-endpoint (str server-root "login")]
      {:username       username
       :password       password
       :endpoint       endpoint
       :login-endpoint login-endpoint})))

;; FIXME: Caution!  Do not commit credentials.
(def ^:dynamic *server-info* (set-server-info nil nil "https://nuv.la/"))

(defn strip-fields [m]
  (dissoc m :id :created :updated :acl :operations))

(deftest event-lifecycle-async
  (async done
    (go
      (if *server-info*
        (let [{:keys [username password endpoint login-endpoint]} *server-info*]

          ;; check configuration sanity
          (is username)
          (is password)
          (is endpoint)
          (is login-endpoint)

          ;; get the cloud entry point for server
          (let [cep (<! (t/cloud-entry-point endpoint))]
            (is (map? cep))
            (is (:baseURI cep))
            (is (:events cep))

            ;; log into the server
            (let [[status token] (<! (authn/login-async username password login-endpoint))]
              (is (= 200 status))
              (is (string? token))
              (is (not (s/blank? token)))

              ;; search for events
              (let [events (<! (t/search token cep "events"))]
                (is (map? events))
                (is (:count events))

                ;; add a new event resource
                (let [add-event-resp (t/add token cep "events" example-event)]
                  (is (map? add-event-resp))
                  (is (= 201 (:status add-event-resp)))
                  (is (not (s/blank? (:message add-event-resp))))
                  (is (not (s/blank? (:resource-id add-event-resp))))

                  ;; read the event back; do a search for all events
                  (let [event-id (:resource-id add-event-resp)
                        read-event (t/get token cep event-id)
                        events (t/search token cep "events")]
                    (is (= (strip-fields example-event) (strip-fields read-event)))
                    (is (pos? (:count events)))

                    ;; events cannot be edited
                    (is (thrown? js/Error (t/edit token cep event-id read-event)))

                    ;; delete the event and ensure that it is gone
                    (let [delete-resp (t/delete token cep event-id)]
                      (is (= 200 (:status delete-resp)))
                      (try
                        (let [get-resp (t/get token cep event-id)]
                          (is (nil? get-resp)))
                        (catch js/Error ex
                          (let [resp (ex-data ex)]
                            (is (= 404 (:status resp)))))))))))))))
    (done)))

;;
;; CAUTION: If too many 'is' tests are added, the clojurescript compiler
;; may cause the stack to overflow.  This is apparently related to the issue
;; http://dev.clojure.org/jira/browse/ASYNC-40
;; The immediate solution is to eliminate some of the less useful tests.
;;
(deftest attribute-lifecycle-async
  (async done
    (go
      (if *server-info*
        (let [{:keys [username password endpoint login-endpoint]} *server-info*]

          ;; check configuration sanity
          (is username)
          (is password)
          (is endpoint)
          (is login-endpoint)

          ;; get the cloud entry point for server
          (let [cep (<! (t/cloud-entry-point endpoint))]
            (is (map? cep))
            (is (:baseURI cep))
            (is (:serviceAttributes cep))

            ;; log into the server
            (let [[status token] (<! (authn/login-async username password login-endpoint))]
              (is (= 200 status))
              (is (string? token))
              (is (not (s/blank? token)))

              ;; search for service-attributes
              (let [attrs (t/search token cep "serviceAttributes")]
                #_(is (map? attrs))
                (is (:count attrs))

                ;; add a new attribute resource
                (let [add-attr-resp (<! (t/add token cep "serviceAttributes" example-attr))]
                  #_(is (map? add-attr-resp))
                  (is (= 201 (:status add-attr-resp)))
                  (is (not (s/blank? (:message add-attr-resp))))
                  (is (not (s/blank? (:resource-id add-attr-resp))))

                  ;; read the attribute back; do a search for all attributes
                  (let [attr-id (:resource-id add-attr-resp)
                        read-attr (<! (t/get token cep attr-id))
                        attrs (<! (t/search token cep "serviceAttributes"))]
                    (is (= (strip-fields example-attr) (strip-fields read-attr)))
                    #_(is (pos? (:count attrs)))

                    ;; try editing the attribute
                    (let [updated-attr (assoc read-attr :major-version 10)
                          edit-resp (<! (t/edit token cep attr-id updated-attr))
                          reread-attr (<! (t/get token cep attr-id))]
                      #_(is (map? edit-resp))
                      (is (= (:body edit-resp) reread-attr)) ;; FIXME: double nesting of response!
                      (is (= (strip-fields updated-attr) (strip-fields reread-attr))))

                    ;; delete the attribute and ensure that it is gone
                    (let [delete-resp (<! (t/delete token cep attr-id))]
                      (is (= 200 (:status delete-resp)))
                      (try
                        (let [get-resp (<! (t/get token cep attr-id))]
                          (is (nil? get-resp)))
                        (catch js/Error ex
                          (let [resp (ex-data ex)]
                            (is (= 404 (:status resp)))))))))))))))
    (done)))
