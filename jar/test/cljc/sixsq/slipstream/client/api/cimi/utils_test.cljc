(ns sixsq.slipstream.client.api.cimi.utils-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.cimi.utils :as t]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.json :as json]
    [clojure.core.async :refer #?(:clj  [chan <! >! go <!!]
                                  :cljs [chan <! >!])]
    [clojure.test :refer [deftest is are testing run-tests #?(:cljs async)]]))

(def test-cep {:id               "cloud-entry-point"
               :resourceURI      "http://schemas.dmtf.org/cimi/2/CloudEntryPoint"
               :created          "2015-09-01T20:36:16.891Z"
               :updated          "2015-09-01T20:36:16.891Z"
               :baseURI          "https://localhost:8201/api/"
               :attributes       {:href "attribute"}
               :connectors       {:href "connector"}
               :events           {:href "event"}
               :licenses         {:href "license"}
               :licenseTemplates {:href "license-template"}
               :usages           {:href "usage"}
               :networkServices  {:href "network-service"}
               :serviceOffers    {:href "service-offer"}
               :usageRecords     {:href "usage-record"}
               :acl              {:owner {:principal "ADMIN", :type "ROLE"}
                                  :rules [{:principal "ANON", :type "ROLE", :right "VIEW"}]}})

(def ops-example {:operations [{:rel  "add"
                                :href "add"}
                               {:rel  "edit"
                                :href "edit"}
                               {:rel  "delete"
                                :href "delete"}]})

(def other-map {:alpha true
                :beta  true})

(def cimi-map {:$first   1
               :$last    2
               :$select  "alpha"
               :$filter  "a=2"
               :$expand  "beta"
               :$orderby "alpha:asc"})

(deftest cimi-params-handling
  (let [m (merge other-map cimi-map)]
    (is (nil? (t/remove-cimi-params "BAD")))
    (is (nil? (t/remove-cimi-params nil)))
    (is (= {} (t/remove-cimi-params {})))
    (is (= other-map (t/remove-cimi-params m)))

    (is (nil? (t/select-cimi-params "BAD")))
    (is (nil? (t/select-cimi-params nil)))
    (is (= {} (t/select-cimi-params {})))
    (is (= cimi-map (t/select-cimi-params m)))))

(deftest check-state-updates
  (is (nil? (t/update-state (atom nil) nil "OK")))
  (are [expected state input] (= expected (t/update-state (atom state) :token input))
                              nil nil nil
                              nil {} nil
                              {:token "OK"} {} "OK"
                              {:token "OK"} {:token "BAD"} "OK"))

(deftest check-unauthorized
  (let [v (t/unauthorized)
        data (ex-data (first v))]
    (is (vector? v))
    (is (nil? (second v)))
    (is (= 403 (:status data)))
    (is (= "unauthorized" (:message data)))
    (is (= nil (:resource-id data))))
  (let [id "resource/uuid"
        v (t/unauthorized id)
        data (ex-data (first v))]
    (is (= id (:resource-id data)))))

(deftest correct-collection-urls
  (is (nil? (t/get-collection-url nil nil)))
  (is (nil? (t/get-collection-url nil "connectors")))
  (are [x y] (= x (t/get-collection-url test-cep y))
             nil nil
             nil "unknownResources"
             nil :unknownResources
             "https://localhost:8201/api/attribute" "attributes"
             "https://localhost:8201/api/attribute" :attributes
             "https://localhost:8201/api/connector" "connectors"
             "https://localhost:8201/api/connector" :connectors
             "https://localhost:8201/api/event" "events"
             "https://localhost:8201/api/event" :events
             "https://localhost:8201/api/license" "licenses"
             "https://localhost:8201/api/license" :licenses
             "https://localhost:8201/api/license-template" "licenseTemplates"
             "https://localhost:8201/api/license-template" :licenseTemplates
             "https://localhost:8201/api/usage" "usages"
             "https://localhost:8201/api/usage" :usages
             "https://localhost:8201/api/network-service" "networkServices"
             "https://localhost:8201/api/network-service" :networkServices
             "https://localhost:8201/api/service-offer" "serviceOffers"
             "https://localhost:8201/api/service-offer" :serviceOffers
             "https://localhost:8201/api/usage-record" "usageRecords"
             "https://localhost:8201/api/usage-record" :usageRecords))

(deftest check-extract-op-url-tests
  (let [baseURI "https://localhost:8201/api/"
        body ops-example
        json (json/edn->json body)
        req {:body json}]
    (are [op] (= (str baseURI op) (first (eduction (t/extract-op-url op baseURI) [req])))
              "add"
              "edit"
              "delete")))

(defn extract-op-url-tests
  ([]
   (extract-op-url-tests nil))
  ([done]
   (go
     (let [baseURI "https://localhost:8201/api/"
           body ops-example
           json (json/edn->json body)]
       (let [c (chan 1 (t/extract-op-url "add" baseURI) identity)
             _ (>! c {:body json})
             result (<! c)]
         (is (= (str baseURI "add") result)))
       (let [c (chan 1 (t/extract-op-url "edit" baseURI) identity)
             _ (>! c {:body json})
             result (<! c)]
         (is (= (str baseURI (name "edit")) result)))
       (let [c (chan 1 (t/extract-op-url "delete" baseURI) identity)
             _ (>! c {:body json})
             result (<! c)]
         (is (= (str baseURI "delete") result))))
     (if done (done)))))

(deftest check-extract-op-url-tests-with-chan
  #?(:clj  (<!! (extract-op-url-tests))
     :cljs (async done (extract-op-url-tests done))))
