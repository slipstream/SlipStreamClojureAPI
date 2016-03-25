(ns com.sixsq.slipstream.clj-client.run-impl-test
  (:refer-clojure :exclude [get])
  (:require 
   [com.sixsq.slipstream.clj-client.lib.run-impl :as t]
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])))

(deftest test-to-ids
  (are [x y] (= x (t/extract-ids y))
             ["1" "2" "3"] "node.1,node.2,node.3"
             ["1" "2" "3"] "node.1 ,  node.2, node.3"
             [] "node2"
             ["123"] "node.123"
             ["2"] "node.1.2"
             ["3"] "node, node.3"))

(deftest test-build-param-url
  (is (= "run/123/foo.1:bar" (t/to-param-uri "123" "foo" 1 "bar"))))

(deftest test-into-params
  (is (= nil (t/merge-request)))
  (is (= nil (t/merge-request nil)))
  (is (= {:b 2} (t/merge-request {:b 2})))
  (is (= {:h {:a 1 :b 2}} (t/merge-request {:h {:a 1}} {:h {:b 2}}))))
