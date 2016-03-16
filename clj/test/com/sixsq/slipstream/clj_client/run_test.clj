(ns com.sixsq.slipstream.clj-client.run-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.run :refer [set-run-config!]]
            [com.sixsq.slipstream.clj-client.run :refer :all]))

(def run-uuid "123")
(set-run-config! {:username "user" :password "pass"})

(deftest test-get-comp-ids
  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [_ _ _ _] "")}
    #(is (= '() (get-comp-ids run-uuid "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [_ _ _ _] "1,2,3")}
    #(is (= '("1" "2" "3") (get-comp-ids run-uuid "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-param (fn [_ _ _ _] "30,10,21")}
    #(is (= '("10" "21" "30") (get-comp-ids run-uuid "foo")))))

(deftest test-scale-up
  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.http-impl/post (fn [_ _] {:body "comp.1,comp.2"})}
    #(is (= ["comp.1" "comp.2"] (scale-up run-uuid comp 2))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.http-impl/post (fn [_ _] {:body "comp.1,comp.2"})
                   #'com.sixsq.slipstream.clj-client.http-impl/put (fn [_ _] nil)}
    #(is (= ["comp.1" "comp.2"] (scale-up run-uuid comp 2 {"foo" 1 "bar" 2})))))
