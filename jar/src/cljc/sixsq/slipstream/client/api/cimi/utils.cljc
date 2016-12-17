(ns sixsq.slipstream.client.api.cimi.utils
  "Provides utilities that support the SCRUD actions for CIMI resources.
   Although these functions are public, they are not part of the public
   API and may change without notice."
  (:refer-clojure :exclude [read])
  (:require
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.json :as json]))

(defn get-collection-url [cep collection-name]
  (let [collection-name (keyword collection-name)
        baseURI (:baseURI cep)]
    (str baseURI (-> cep collection-name :href))))

(defn extract-op-url
  "Transducer that extracts the operation URL for the given operation.
   The return value is a possibly empty list."
  [op baseURI]
  (comp
    (map e/throw-if-error)
    (map :body)
    (map json/json->edn)
    (map :operations)
    cat
    (map (juxt :rel :href))
    (filter (fn [[k _]] (= op k)))
    (map (fn [[_ v]] (str baseURI v)))))



