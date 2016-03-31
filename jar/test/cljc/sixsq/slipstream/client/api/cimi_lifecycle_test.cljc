;; FIXME: Needs to be cleaned up for clojurescript, Thread, login, ...
(ns sixsq.slipstream.client.api.cimi-lifecycle-test
  "Runs lifecycle tests for CIMI resources against a live server.  If no
   user credentials are provided, the lifecycle tests are 'no-ops'.  To run
   these tests (typically from the REPL), do the following:

   ```clojure
   (in-ns 'sixsq.slipstream.client.api.cimi-lifecycle-test)
   (set-server-info \"my-username\" \"my-password\" \"my-server-root\")
   (run-tests)
   ```
   These tests make an assumption about the location of the cloud entry
   point URL and the login URL relative to the value given for
   \"my-server-root\".  In the future, all of the required URLs will be
   available from the cloud entry point.

   **NOTE**: The value for \"my-server-root\" must end with a slash!
   "
  (:require
    [sixsq.slipstream.client.api.cimi :as t]
    [sixsq.slipstream.client.api.authn :as authn]
    [superstring.core :as s]
    #?(:clj
    [clojure.test :refer [deftest is are testing run-tests]]
       :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])))

(def ^:dynamic *server-info* nil)

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
   :resourceURI   "http://sixsq.com/slipstream/1/Attribute"
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
  (let [endpoint (str server-root "api/cloud-entry-point")
        login-endpoint (str server-root "login")
        base-uri (str server-root "api/")]
    (->> {:username       username
          :password       password
          :endpoint       endpoint
          :login-endpoint login-endpoint
          :base-uri       base-uri}
         (constantly)
         (alter-var-root #'*server-info*))))

(defn strip-fields [m]
  (dissoc m :id :created :updated :acl :operations))

(deftest event-lifecycle
  (if *server-info*
    (let [{:keys [username password endpoint login-endpoint base-uri]} *server-info*]

      ;; check configuration sanity
      (is username)
      (is password)
      (is endpoint)
      (is login-endpoint)
      (is base-uri)

      ;; log into the server
      (let [token (authn/login username password login-endpoint)]
        (is (string? token))
        (is (not (s/blank? token)))

        ;; get the cloud entry point for server
        (let [cep (t/cloud-entry-point token endpoint)]
          (is (map? cep))
          (is (:baseURI cep))
          (is (:events cep))

          ;; fix cloud entry point and search for events
          (let [cep (assoc cep :baseURI base-uri)           ;; workaround server bug
                events (t/search token cep "events")]
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
                (is (nil? (t/edit token cep event-id read-event)))

                ;; delete the event and ensure that it is gone
                (let [delete-resp (t/delete token cep event-id)
                      _ (Thread/sleep 10000)]               ;; avoid 504 on server
                  (is (thrown? Exception (t/get token cep event-id))))))))))))

(deftest attribute-lifecycle
  (if *server-info*
    (let [{:keys [username password endpoint login-endpoint base-uri]} *server-info*]

      ;; check configuration sanity
      (is username)
      (is password)
      (is endpoint)
      (is login-endpoint)
      (is base-uri)

      ;; log into the server
      (let [token (authn/login username password login-endpoint)]
        (is (string? token))
        (is (not (s/blank? token)))

        ;; get the cloud entry point for server
        (let [cep (t/cloud-entry-point token endpoint)]
          (is (map? cep))
          (is (:baseURI cep))
          (is (:attribute cep))                             ;; should be :attributes

          ;; fix cloud entry point and search for events
          (let [cep (assoc cep :baseURI base-uri)           ;; workaround server bug
                attrs (t/search token cep "attribute")]     ;; should be "attributes"
            (is (map? attrs))
            (is (:count attrs))

            ;; add a new attribute resource
            (let [add-attr-resp (t/add token cep "attribute" example-attr)]
              (is (map? add-attr-resp))
              (is (= 201 (:status add-attr-resp)))
              (is (not (s/blank? (:message add-attr-resp))))
              (is (not (s/blank? (:resource-id add-attr-resp))))

              ;; read the event back; do a search for all events
              (let [attr-id (:resource-id add-attr-resp)
                    read-attr (t/get token cep attr-id)
                    attrs (t/search token cep "attribute")]
                (is (= (strip-fields example-attr) (strip-fields read-attr)))
                (is (pos? (:count attrs)))

                ;; try editing the attribute
                (let [updated-attr (assoc read-attr :major-version 10)
                      edit-resp (t/edit token cep attr-id updated-attr)
                      reread-attr (t/get token cep attr-id)]
                  (is (map? edit-resp))
                  (is (= 200 (:status edit-resp)))
                  (is (not (s/blank? (:message edit-resp))))
                  (is (not (s/blank? (:resource-id edit-resp))))
                  (is (= (strip-fields updated-attr) (strip-fields reread-attr))))

                ;; delete the attribute and ensure that it is gone
                (let [delete-resp (t/delete token cep attr-id)
                      _ (Thread/sleep 10000)]               ;; avoid 504 on server
                  (is (thrown? Exception (t/get token cep attr-id))))))))))))