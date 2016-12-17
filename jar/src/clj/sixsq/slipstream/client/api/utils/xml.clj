(ns sixsq.slipstream.client.api.utils.xml
  "Utilities for dealing with XML.

   WARNING: the clojure representation of translated XML documents are not
   identical between Clojure and ClojureScript!"
  (:refer-clojure :exclude [get])
  (:require
    [sixsq.slipstream.client.api.utils.common :as cu]
    [sixsq.slipstream.client.api.utils.error :as e])
  (:import (org.json XML)))

(defn xml->edn [xml]
  (->> xml
       (XML/toJSONObject)
       str
       cu/str->json))

(defn body-as-xml
  "transducer that extracts the body of a response and parses
   the result as XML"
  []
  (comp
    (map e/throw-if-error)
    (map :body)
    (map xml->edn)))


