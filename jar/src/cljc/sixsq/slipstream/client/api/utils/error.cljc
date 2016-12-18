(ns sixsq.slipstream.client.api.utils.error)

(defn error? [e]
  (instance? #?(:clj Exception :cljs js/Error) e))

(defn throw-if-error [e]
  (if (error? e)
    (throw e)
    e))
