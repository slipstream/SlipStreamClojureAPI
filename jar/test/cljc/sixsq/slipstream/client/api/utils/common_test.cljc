(ns sixsq.slipstream.client.api.utils.common-test
  (:require
    [sixsq.slipstream.client.api.utils.common :as t]
    [clojure.test :refer [deftest is are testing run-tests]]))

(deftest test-ensure-url
  (let [baseUrl "https://nuv.la"
        fullUrl "https://nuv.la/api/resource/id"]
    (is (= (t/ensure-url baseUrl fullUrl) fullUrl))
    (is (= (t/ensure-url baseUrl "/api/resource/id") fullUrl))
    (is (= (t/ensure-url-slash baseUrl fullUrl) fullUrl))
    (is (= (t/ensure-url-slash baseUrl "api/resource/id") fullUrl))))
