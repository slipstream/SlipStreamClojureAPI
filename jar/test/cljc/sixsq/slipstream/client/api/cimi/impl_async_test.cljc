(ns sixsq.slipstream.client.api.cimi.impl-async-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.cimi.impl-async :as t]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.json :as json]
    [clojure.core.async :refer #?(:clj  [chan <! >! go <!!]
                                  :cljs [chan <! >!])]
    [clojure.test :refer [deftest is are testing run-tests #?(:cljs async)]]))

(def other-map {:alpha true
                :beta  true})

(def cimi-map {:$first   1
               :$last    2
               :$select  "alpha"
               :$filter  "a=2"
               :$expand  "beta"
               :$orderby "alpha:asc"})

(deftest cimi-params-handling
  (let [m (merge other-map cimi-map)]
    (is (nil? (t/remove-cimi-params nil)))
    (is (= {} (t/remove-cimi-params {})))
    (is (= other-map (t/remove-cimi-params m)))

    (is (nil? (t/select-cimi-params nil)))
    (is (= {} (t/select-cimi-params {})))
    (is (= cimi-map (t/select-cimi-params m)))))
