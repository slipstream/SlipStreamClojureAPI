(ns ^{:no-doc true} sixsq.slipstream.client.impl.utils.xml
  "Utilities for dealing with XML.  WARNING: the clojure representation
   of translated XML documents are not identical between Clojure and
   ClojureScript!"
  (:require
    [sixsq.slipstream.client.impl.utils.error :as e]
    [clojure.walk :as walk]
    [clojure.string :as str]))

(declare xml->json)

(defn parse-xml [xml]
  (let [parser (js/DOMParser.)]
    (.parseFromString parser xml "text/xml")))

(defn node-name [node]
  (.-nodeName node))

(defn node-value [node]
  (.-nodeValue node))

(defn attributes [node]
  (for [i (range (.-attributes.length node))] (.attributes.item node i)))

(defn children [node]
  (for [i (range (.-childNodes.length node))] (.childNodes.item node i)))

(defn attribute-map [element]
  (if (and element (= 1 (.-nodeType element)))
    (into {} (map (juxt node-name node-value) (attributes element)))))

(defn unwrap-single-item [coll]
  (if (= 1 (count coll))
    (first coll)
    coll))

(defn blank-text-node? [[k v]]
  (and (= "#text" k) (str/blank? v)))

(defn child-map [element]
  (if (.hasChildNodes element)
    (->> (children element)
         (map (juxt node-name xml->json))
         (remove blank-text-node?)
         (group-by first)
         (map (fn [[k v]] [k (unwrap-single-item (map second v))]))
         (into {}))))

(defn xml->json [node]
  (if node
    (if (= (.-nodeType node) 3)
      (.-nodeValue node)                                    ;; text node
      (merge (attribute-map node) (child-map node)))))

(defn xml->edn [xml]
  (->> xml
       parse-xml
       xml->json
       walk/keywordize-keys))

(defn body-as-xml
  "transducer that extracts the body of a response and parses
   the result as XML"
  []
  (comp
    (map e/throw-if-error)
    (map :body)
    (map xml->edn)))
