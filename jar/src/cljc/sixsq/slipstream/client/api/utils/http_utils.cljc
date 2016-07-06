(ns sixsq.slipstream.client.api.utils.http-utils)

(def ^:const http-lib-insecure-key :kvlt.platform/insecure?)

(defn process-req
  [req]
  (if (contains? req :insecure?)
    (-> req
        (merge {http-lib-insecure-key (:insecure? req)})
        (dissoc :insecure?))
    req))

