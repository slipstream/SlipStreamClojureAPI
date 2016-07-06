(ns sixsq.slipstream.client.api.utils.http-utils-test
  (:require
    [sixsq.slipstream.client.api.utils.http-utils :as h]
    #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
       :cljs [cljs.test :refer-macros [deftest is testing run-tests]])))

(deftest test-process-req
  (let [req (h/process-req {})]
    (is (contains? req h/http-lib-insecure-key))
    (is (not (h/http-lib-insecure-key req))))
  (let [req (h/process-req {:a 1})]
    (is (contains? req :a))
    (is (contains? req h/http-lib-insecure-key))
    (is (not (h/http-lib-insecure-key req))))
  (let [req (h/process-req {:insecure? true})]
    (is (not (contains? req :insecure?)))
    (is (contains? req h/http-lib-insecure-key))
    (is (true? (h/http-lib-insecure-key req))))
  (let [req (h/process-req {:insecure? false})]
    (is (not (contains? req :insecure?)))
    (is (contains? req h/http-lib-insecure-key))
    (is (false? (h/http-lib-insecure-key req)))))

