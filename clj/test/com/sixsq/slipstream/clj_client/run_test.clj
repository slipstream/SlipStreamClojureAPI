(ns com.sixsq.slipstream.clj-client.run-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.run :refer :all]))

(deftest test-in-final-states?

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-state (fn [] "Done")}
    #(is (= #'com.sixsq.slipstream.clj-client.run/in-final-states?) true))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-state (fn [] "Running")}
    #(is (= #'com.sixsq.slipstream.clj-client.run/in-final-states?) false))

  (is (= (#'com.sixsq.slipstream.clj-client.run/in-final-states? "Done") true)))

(deftest test-get-node-ids
  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "")}
    #(is (= '() (get-instance-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "1,2,3")}
    #(is (= '("1" "2" "3") (get-instance-ids "foo"))))

  (with-redefs-fn {#'com.sixsq.slipstream.clj-client.run/get-rtp (fn [a b c] "30,10,21")}
    #(is (= '("10" "21" "30") (get-instance-ids "foo")))))

