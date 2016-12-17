(ns sixsq.slipstream.client.api.cimi.utils-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.cimi.utils :as t]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.json :as json]
    [clojure.core.async :refer #?(:clj [chan <! >! go <!!]
                                  :cljs [chan <! >!])]
    [clojure.test :refer [deftest is are testing run-tests #?(:cljs async)]]))

(def test-cep {:id               "cloud-entry-point"
               :resourceURI      "http://schemas.dmtf.org/cimi/2/CloudEntryPoint"
               :created          "2015-09-01T20:36:16.891Z"
               :updated          "2015-09-01T20:36:16.891Z"
               :baseURI          "https://localhost:8201/api/"
               :attribute        {:href "attribute"}
               :connectors       {:href "connector"}
               :events           {:href "event"}
               :licenses         {:href "license"}
               :licenseTemplate  {:href "license-template"}
               :usages           {:href "usage"}
               :network-services {:href "network-service"}
               :service-info     {:href "service-info"}
               :usage-records    {:href "usage-record"}
               :acl              {:owner {:principal "ADMIN", :type "ROLE"}
                                  :rules [{:principal "ANON", :type "ROLE", :right "VIEW"}]}})

(def ops-example {:operations [{:rel  "add"
                                :href "add"}
                               {:rel  "edit"
                                :href "edit"}
                               {:rel  "delete"
                                :href "delete"}]})

(deftest correct-collection-urls
  (are [x y] (= x (t/get-collection-url test-cep y))
             "https://localhost:8201/api/attribute" "attribute"
             "https://localhost:8201/api/connector" "connectors"
             "https://localhost:8201/api/event" "events"
             "https://localhost:8201/api/license" "licenses"
             "https://localhost:8201/api/license-template" "licenseTemplate"
             "https://localhost:8201/api/usage" "usages"
             "https://localhost:8201/api/network-service" "network-services"
             "https://localhost:8201/api/service-info" "service-info"
             "https://localhost:8201/api/usage-record" "usage-records"))

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
