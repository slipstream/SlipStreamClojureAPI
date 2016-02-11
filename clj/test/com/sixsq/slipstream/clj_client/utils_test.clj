(ns com.sixsq.slipstream.clj-client.utils-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.clj-client.utils :refer :all]))


(deftest test-in?
  (is (true? (in? :a [:a :b])))
  (is (true? (in? :a #{:a :b})))
  (is (true? (in? :a '(:a :b))))

  (is (false? (in? :a [:b])))
  (is (false? (in? :a #{:b})))
  (is (false? (in? :a '(:b)))))

(deftest test-url-join
  (is (= "" (url-join)))
  (is (= "" (url-join [])))
  (is (= "a/b" (url-join ["a" "b"])))
  (is (= "http://example.com/r1/id1" (url-join ["http://example.com" "r1" "id1"]))))
