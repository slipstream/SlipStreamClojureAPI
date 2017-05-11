(ns sixsq.slipstream.client.api.cimi.utils
  "Provides utilities that support the SCRUD actions for CIMI resources.
   Although these functions are public, they are not part of the public
   API and may change without notice."
  (:refer-clojure :exclude [read])
  (:require
    [clojure.set :as set]
    [sixsq.slipstream.client.api.utils.error :as e]
    [sixsq.slipstream.client.api.utils.json :as json]))

(def ^:const cimi-params #{:$first :$last :$filter :$select :$expand :$orderby})

(defn select-cimi-params
  "Strips keys from the provided map except for CIMI request parameters and
   returns that map. Returns nil if something other than a map is provided."
  [m]
  (when (map? m)
    (select-keys m cimi-params)))

(defn remove-cimi-params
  "Strips the CIMI request parameters from the provided map and returns the
   updated map. Returns nil if something other than a map is provided."
  [m]
  (when (map? m)
    (select-keys m (set/difference (set (keys m)) cimi-params))))

(defn update-state
  "If the token is not nil, then updates the value of the :token key inside
   the provided state atom. If the token is nil, then the state atom is not
   updated. Returns the new value of the atom or nil if no change was made."
  [state k v]
  (when (and state k v)
    (swap! state merge {k v})))

(defn error-tuple
  "Produces a response tuple containing the exception/error and a nil cookie
   value. Used to provide a uniform response from channels, even when errors
   occur."
  [error]
  [error nil])

(defn response-tuple
  "This extracts the HTTP response body (rendered as keywordized EDN) and the
   value of the set-cookie header and returns a tuple with the two values in
   that order."
  [{:keys [body headers] :as response}]
  (println "DEBUG RESPONSE")
  (clojure.pprint/pprint response)
  (println "END DEBUG")
  (let [token (get headers "set-cookie")]
    [(json/json->edn body) token]))

(defn response-xduce
  "Transducer that extracts the HTTP response body and any set-cookie header
   value, returning a tuple of those values. If an error occurs, the error will
   be returned as the first element of the tuple."
  []
  (comp
    (map e/throw-if-error)
    (map response-tuple)))

(defn unauthorized
  "Returns a tuple containing an exception that has a 403 unauthorized code
   and a reference to the resource. The second element of the tuple is nil."
  [& [resource-id]]
  (let [msg "unauthorized"
        data (merge {:status 403, :message msg}
                    (when resource-id {:resource-id resource-id}))
        e (ex-info msg data)]
    [e nil]))

(defn get-collection-url
  "Extracts the absolute URL for a the named collection from the cloud entry
   point. The collection name can be provided either as a string or a keyword.
   The capitalization of the collection name is significant; normally the value
   is camel-cased and has a trailing 's'. Returns nil if the collection does
   not exist."
  [cep collection-name]
  (when (and cep collection-name)
    (let [collection (keyword collection-name)
          baseURI (:baseURI cep)]
      (when-let [href (-> cep collection :href)]
        (str baseURI href)))))

(defn extract-op-url
  "Transducer that extracts the operation URL for the given operation. The
   return value is a possibly empty list."
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
