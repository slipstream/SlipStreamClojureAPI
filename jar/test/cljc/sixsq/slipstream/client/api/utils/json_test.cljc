(ns sixsq.slipstream.client.api.utils.json-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    [sixsq.slipstream.client.api.utils.json :as t]
    [sixsq.slipstream.client.api.utils.error :as e]
    [clojure.core.async :refer #?(:clj [chan <! >! go <!!]
                                  :cljs [chan <! >!])]
    [clojure.test :refer [deftest is are testing run-tests #?(:cljs async)]]))

(def body-example {:alpha 1
                   :beta  "2"
                   :gamma 3.0
                   :delta false})

(deftest check-body-as-json-trans
  (let [body body-example
        json (t/edn->json body)
        req {:body json}]
    (is (= body-example (first (eduction (t/body-as-json) [req]))))))

(defn body-tests
  ([]
   (body-tests nil))
  ([done]
   (go
     (let [body body-example
           json (t/edn->json body)
           c (chan 1 (t/body-as-json) identity)
           _ (>! c {:body json})
           result (<! c)]
       (is (= body result)))
     (if done (done)))))

(deftest check-body-as-json
  #?(:clj  (<!! (body-tests))
     :cljs (async done (body-tests done))))

(defn exception-tests
  ([]
   (exception-tests nil))
  ([done]
   (go
     (let [msg "msg-to-match"
           data {:dummy "data"}
           ex (ex-info msg data)
           c (chan 1 (t/body-as-json) identity)
           _ (>! c ex)
           result (<! c)]
       (is (e/error? result))
       (is (= msg #?(:clj  (.getMessage result)
                     :cljs (.-message result))))
       (is (= data (ex-data result))))
     (if done (done)))))

(deftest check-body-as-json-error
  #?(:clj  (<!! (exception-tests))
     :cljs (async done (exception-tests done))))
