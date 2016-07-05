(ns sixsq.slipstream.client.api.utils.http-utils-test
  (:require
    [sixsq.slipstream.client.api.utils.http-utils :as h]
    #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
       :cljs [cljs.test :refer-macros [deftest is testing run-tests]])))

(deftest test-process-req
  (is (= {} (h/process-req {})))
  (is (= {:a 1} (h/process-req {:a 1})))
  (let [req (h/process-req {:insecure? true})]
    (is (not (contains? req :insecure?)))
    (is (contains? req h/http-lib-insecure-key))
    (is (= true (h/http-lib-insecure-key req)))))

