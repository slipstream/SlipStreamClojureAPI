(ns com.sixsq.slipstream.clj-client.run-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.run :refer :all]))

(deftest test-get-comp-ids
  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [a b c] "")}
    #(is (= '() (get-comp-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [a b c] "1,2,3")}
    #(is (= '("1" "2" "3") (get-comp-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [a b c] "30,10,21")}
    #(is (= '("10" "21" "30") (get-comp-ids "foo")))))

