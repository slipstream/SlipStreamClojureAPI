(ns com.sixsq.slipstream.clj-client.run-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.run :refer :all]))

(deftest test-get-node-ids
  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "")}
    #(is (= '() (get-instance-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "1,2,3")}
    #(is (= '("1" "2" "3") (get-instance-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "30,10,21")}
    #(is (= '("10" "21" "30") (get-instance-ids "foo")))))

