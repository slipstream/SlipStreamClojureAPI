(ns sixsq.slipstream.client.api.impl.cimi
  "Provides utilities that support the SCRUD actions for CIMI resources.
   Although these functions are public, they are not part of the public
   API and may change without notice."
  (:refer-clojure :exclude [read])
  (:require
    [sixsq.slipstream.client.api.utils.http :as http]
    [clojure.walk :as w]
    #?(:clj
    [clj-json.core :as json])
    [superstring.core :as s]))

(def action-uris {:add    "http://sixsq.com/slipstream/1/Action/add"
                  :delete "http://sixsq.com/slipstream/1/Action/delete"
                  :edit   "http://sixsq.com/slipstream/1/Action/edit"})

(def action-keywords {"http://sixsq.com/slipstream/1/Action/add"    :add
                      "http://sixsq.com/slipstream/1/Action/delete" :delete
                      "http://sixsq.com/slipstream/1/Action/edit"   :edit})

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

(defn error? [e]
  (instance? #?(:clj Exception :cljs js/Error) e))

(defn str->json [s]
  #?(:clj  (json/parse-string s)
     :cljs (JSON.parse s)))

(defn json->str [json]
  #?(:clj  (json/generate-string json)
     :cljs (JSON.stringify json)))

(defn json->edn [s]
  (cond
    (nil? s) {}
    (error? s) s
    :else (w/keywordize-keys (str->json s))))

(defn kw->string [kw]
  (if (keyword? kw) (name kw) kw))

(defn edn->json [json]
  (-> (w/postwalk kw->string json)
      (json->str)))

(defn ensure-url [cep url-or-id]
  (if (re-matches #"^((http://)|(https://))" url-or-id)
    url-or-id
    (str (:baseURI cep) url-or-id)))

(defn get-collection-url [cep collection-name]
  (let [collection-name (keyword collection-name)
        baseURI (:baseURI cep)]
    (str baseURI (-> cep collection-name :href))))

(defn action-keyword [s]
  (or (get action-keywords s) (keyword s)))

(defn get-collection-operations [token cep collection-name]
  (let [baseURI (:baseURI cep)
        url (get-collection-url cep collection-name)
        req (req-opts token)]
    (into {}
          (->> (http/get url req)
               :body
               json->edn
               :operations
               (map (juxt :rel :href))
               (map (fn [[k v]] [(action-keyword k) (str baseURI v)]))))))

(defn get-resource-operations [token cep url-or-id]
  (let [baseURI (:baseURI cep)
        url (ensure-url cep url-or-id)
        req (req-opts token)]
    (into {}
          (->> (http/get url req)
               :body
               json->edn
               :operations
               (map (juxt :rel :href))
               (map (fn [[k v]] [(action-keyword k) (str baseURI v)]))))))

(defn error-transducer
  "Creates a transducer that will check for an exception and provide that
   exception as a reduced value when detected."
  []
  (fn [xf]
    (fn
      ([] (xf))
      ([result]
       (println "TRANS 1: " result)
       (xf result))
      ([result input]
       (println "TRANS 2: " result " " input)
       (if (error? input)
         (xf result (reduced input))
         (xf result input))))))

(defn body-as-json
  "transducer that extracts the body of a response and parses
   the result as JSON"
  []
  (comp
    (error-transducer)
    (map :body)
    (map json->edn)))


