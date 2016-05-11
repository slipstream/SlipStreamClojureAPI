(ns sixsq.slipstream.client.api.run-test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.client.api.authn :as a]
            [sixsq.slipstream.client.api.lib.run :refer :all]))

(def run-uuid "123")
(a/set-context! {:username "user" :password "pass"})

(deftest test-get-comp-ids
  (with-redefs-fn {#'sixsq.slipstream.client.api.lib.run/get-param (fn [_ _ _ _] "")}
    #(is (= '() (get-comp-ids run-uuid "foo"))))

  (with-redefs-fn {#'sixsq.slipstream.client.api.lib.run/get-param (fn [_ _ _ _] "1,2,3")}
    #(is (= '("1" "2" "3") (get-comp-ids run-uuid "foo"))))

  (with-redefs-fn {#'sixsq.slipstream.client.api.lib.run/get-param (fn [_ _ _ _] "30,10,21")}
    #(is (= '("10" "21" "30") (get-comp-ids run-uuid "foo")))))

(deftest test-scale-up
  (with-redefs-fn {#'sixsq.slipstream.client.api.utils.http/post (fn [_ _] {:body "comp.1,comp.2"})}
    #(is (= ["comp.1" "comp.2"] (scale-up run-uuid comp 2))))

  (with-redefs-fn {#'sixsq.slipstream.client.api.utils.http/post (fn [_ _] {:body "comp.1,comp.2"})
                   #'sixsq.slipstream.client.api.utils.http/put  (fn [_ _] nil)}
    #(is (= ["comp.1" "comp.2"] (scale-up run-uuid comp 2 {"foo" 1 "bar" 2})))))
