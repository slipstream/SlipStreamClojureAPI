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

(deftest test-scale-up
  (with-redefs-fn {#'clj-http.client/post (fn [_ _] {:body "comp.1,comp.2"})}
    #(is (= ["comp.1" "comp.2"] (scale-up comp 2))))

  (with-redefs-fn {#'clj-http.client/post (fn [_ _] {:body "comp.1,comp.2"})
                   #'clj-http.client/put (fn [_ _] nil)}
    #(is (= ["comp.1" "comp.2"] (scale-up comp 2 {"foo" 1 "bar" 2})))))
