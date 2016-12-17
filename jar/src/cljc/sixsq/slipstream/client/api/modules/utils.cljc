(ns sixsq.slipstream.client.api.modules.utils
  "Provides utilities that support the SCRUD actions for CIMI resources.
   Although these functions are public, they are not part of the public
   API and may change without notice."
  (:refer-clojure :exclude [read])
  (:require
    [sixsq.slipstream.client.api.utils.error :as e]
    #?(:clj [clojure.data.json :as json])))

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
  (if url-or-id
    (if (re-matches #"^((http://)|(https://))" url-or-id)
      url-or-id
      (str endpoint "/" url-or-id))
    endpoint))

(defn body-as-json
  "transducer that extracts the body of a response and parses
   the result as JSON"
  []
  (comp
    (map e/throw-if-error)
    (map :body)
    (map json->edn)))

(defn body-as-string
  "transducer that extracts the body of a response and returns
   the result as a string"
  []
  (comp
    (map e/throw-if-error)
    (map :body)))

(defn extract-children [module]
  (if-let [children (get-in module [:projectModule :children :item])]
    (let [children (if (map? children) [children] children)] ;; may be single item!
      (map :name children))))

(defn fix-module-name [mname]
  (first (map second (re-seq #"module/(.+)/[\d+]+" mname))))

(defn extract-xml-children [xml]
  (->> (re-seq #"resourceUri=\"([^\"]*)\"" xml)
       (map second)
       (map fix-module-name)))
