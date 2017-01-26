(ns sixsq.slipstream.client.api.utils.common
  "Provides utilities that support the SCRUD actions for CIMI resources.
   Although these functions are public, they are not part of the public
   API and may change without notice."
  (:require
    [sixsq.slipstream.client.api.utils.error :as e]
    #?(:clj
    [clojure.data.json :as json]))
  #?(:clj
     (:import (org.json XML))))

(def std-opts {:type             :json
               :accept           :json
               :follow-redirects false})

(defn assoc-token [m token]
  (if token
    (assoc m :headers {:cookie token})
    m))

(defn assoc-body [m body]
  (if body
    (assoc m :body body)
    m))

(defn req-opts
  ([]
   (req-opts nil nil))
  ([token]
   (req-opts token nil))
  ([token body]
   (-> std-opts
       (assoc-token token)
       (assoc-body body))))

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

(defn ensure-url [endpoint url-or-id]
  (if (re-matches #"^((http://)|(https://))" url-or-id)
    url-or-id
    (str endpoint url-or-id)))

(defn ensure-url-slash [endpoint url-or-id]
  (if url-or-id
    (if (re-matches #"^((http://)|(https://))" url-or-id)
      url-or-id
      (str endpoint "/" url-or-id))
    endpoint))

(defn body-as-string
  "transducer that extracts the body of a response and returns
   the result as a string"
  []
  (comp
    (map e/throw-if-error)
    (map :body)))
