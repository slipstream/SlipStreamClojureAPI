(ns com.sixsq.slipstream.clj-client.run-impl-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.run-impl :refer :all]))

(deftest test-to-ids
  (are [x y] (= x (extract-ids y))
             ["1" "2" "3"] "node.1,node.2,node.3"
             ["1" "2" "3"] "node.1 ,  node.2, node.3"
             [] "node2"
             ["123"] "node.123"
             ["2"] "node.1.2"
             ["3"] "node, node.3"))

(deftest test-build-param-url
  (is (= "run/123/foo.1:bar" (to-param-uri "123" "foo" 1 "bar"))))

(deftest test-into-params
  (is (= param-req-params (into-params)))
  (is (= param-req-params (into-params nil)))
  (is (= (merge param-req-params {:b 2}) (into-params {:b 2}))))
