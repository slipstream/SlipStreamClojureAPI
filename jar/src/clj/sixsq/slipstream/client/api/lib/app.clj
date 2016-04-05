(ns sixsq.slipstream.client.api.lib.app
  ""
  (:require
    [superstring.core :as s]
    [sixsq.slipstream.client.api.authn :refer [*context*]]
    [sixsq.slipstream.client.api.impl.crud :as h]))

;;
(def param-refqname "refqname")
(def param-scalable "mutable")
(def param-keep-running "keep-running")
(def param-tags "tags")
(def param-type "type")

(def params-reserved #{param-tags
                       param-keep-running
                       param-type})

(defn assoc-comp-param
  [m k v]
  (let [k (s/trim (name k))]
    (cond
      (s/blank? k) m
      (s/includes? k ":") (assoc m (apply format "parameter--node--%s--%s" (s/split k #":")) (str v))
      :else (assoc m (format "parameter--%s" k) (str v)))))

(defn parse-params
  [params]
  (reduce-kv (fn [m k v] (let [k (s/trim (name k))]
                           (cond
                             (s/blank? k) m
                             (= "scalable" k) (assoc m param-scalable (str v))
                             (contains? params-reserved k) (assoc m k (name v))
                             :else (assoc-comp-param m k v))))
             {} params))

(defn app-uri-to-req-params
  [uri params]
  (assoc params param-refqname (str "module/" uri)))

;;
;; Public API.
(defn deploy
  "Deploys an application identified by `uri` (e.g. `examples/my-app`).
  Deployment and component parameters are expected in `params` map.

  The following reserved deployment parameters are recognised:

  - `{:scalable true|false}` to start a scalable deployment;
  - `{:keep-running :never|:always|:on-error|:on-success}` to influence the runtime and
    termination of the deployment;
  - `{:tags \"comma-separated-list\"}` to identify the deployment.

  Application component parameters should be provided in the following form:

  `{\"comp-name:param-name\" val}`

  E.g., to deploy component on a specific cloud use:

  `{\"comp-name:cloudservice\" \"cloud-connector-name\"}`
  "
  {:doc/format :markdown}
  [uri & [params req]]
  (-> (h/post "run" *context* req (->> params
                                       (parse-params)
                                       (app-uri-to-req-params uri)))
      :headers
      :location))


(defn deploy-comp
  "Deploys a component identified by `uri`.

  Component parameters should be provided in the following form:

  `{\"param-name\" val}`

  For details see documentation to [[deploy]]."
  {:doc/format :markdown}
  [uri & [params req]]
  (deploy uri (assoc params :type "Run") req))


(defn build-comp
  "Builds new component identified by `uri`. For details see documentation
  to [[deploy]]."
  {:doc/format :markdown}
  [uri & [params req]]
  (deploy uri (assoc params :type "Machine") req))

