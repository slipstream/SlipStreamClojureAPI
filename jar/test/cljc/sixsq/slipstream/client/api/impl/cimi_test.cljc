(ns sixsq.slipstream.client.api.impl.cimi-test
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.impl.cimi :as t]
    #?(:clj  [clojure.test :refer [deftest is are testing run-tests]]
       :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])))

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

#?(:clj (deftest check-body-as-json
          (let [body {:alpha 1
                      :beta  "2"
                      :gamma 3.0
                      :delta false}
                json (t/edn->json body)
                result (first (eduction (t/body-as-json) [{:body json}]))]
            (is (= body result)))))

#?(:clj (deftest check-body-as-json-error
          (let [ex (ex-info "an exception to catch" {:dummy "data"})
                result (first (eduction (t/body-as-json) [ex]))]
            (println ex)
            (println result)
            (is (= ex result)))))
