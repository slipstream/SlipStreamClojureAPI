(ns ^{:no-doc true} sixsq.slipstream.client.impl.utils.json
  "Utilities for handling JSON data."
  (:require
    #?(:clj
    [clojure.data.json :as json])
    [sixsq.slipstream.client.impl.utils.error :as e]))

(defn str->json [s]
  #?(:clj  (json/read-str s :key-fn keyword)
     :cljs (js->clj (JSON.parse s) :keywordize-keys true)))

(defn edn->json [json]
  #?(:clj  (json/write-str json)
     :cljs (JSON.stringify (clj->js json))))

(defn json->edn [s]
  (cond
    (nil? s) {}
    (e/error? s) s
    :else (str->json s)))

(defn body-as-json
  "transducer that extracts the body of a response and parses
   the result as JSON"
  []
  (comp
    (map e/throw-if-error)
    (map :body)
    (map json->edn)))
