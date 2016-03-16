(ns com.sixsq.slipstream.clj-client.run
  "The `run` namespace contains functions for interacting with SlipStream runs.

  It allows users to

   * Scale the components of the running application up and down,
   * Query the state of the run,
   * Query and set parameters of the application components and their instances,
   * Query parameters from global namespace (`ss:`), and
   * Terminate the run.

  Timeouts and intervals are in seconds.

  Below is the terminology with examples.

  There are three types of parameters on a run

   * Global parameter - `ss:tags`
   * Application component parameter - `webapp:ids`
   * Application component instance parameter - `webapp.1:hostname`

  where

   * `ss` is the global namespace of the run,
   * `webapp` is the name of the application component, which is refered to as `comp`
     in the API,
   * `webapp.1` is the name of the instance `1` of the application component `webapp`.
  "
  (:require
    [com.sixsq.slipstream.clj-client.utils :as u]
    [com.sixsq.slipstream.clj-client.run-impl :as ri]
    [superstring.core :as s]
    [clojure.tools.logging :as log]
    [clojure.data.xml :as xml])
  (:use [clojure.walk :only [stringify-keys]]))

(def default-config {:service-url "https://nuv.la"})

(def ^:dynamic *config* default-config)

;;
;; Public library API.

;; Should be called to provide service URL and credentials.
(defn set-run-config!
  "The following fields are expected.

  {
    :service-url \"https://nuv.la\"

    :username     nil
    :password     nil

    :cookie       nil
  }
  If :cookie is provided it's prefered over the :username/:password.
  "
  [config]
  ;; FIXME: bind instead
  (alter-var-root #'*config* (fn [_] (merge default-config config))))


;; Getters and setters of component, component instance and global parameters.
(defn get-param
  "Get parameter 'param' of application component instance 'comp.id' in run `run-uuid`
  as 'run-uuid/comp.id:param'.
  When 'id' is nil, gets parameter of the application component as 'run-uuid/comp:param'."
  [run-uuid comp id param & [req]]
  (ri/get (ri/to-param-uri run-uuid comp id param) *config* req))

(defn set-param
  "Set parameter 'param' to 'value' on application component instance 'comp.id'."
  [run-uuid comp id param value & [req]]
  (ri/put
    (ri/to-param-uri run-uuid comp id param) value *config* req))

(defn set-params
  "Given a map of parameters 'params', set them on application component
  instance 'run-uuid/comp.id'."
  [run-uuid comp id params & [req]]
  (->> params
       stringify-keys
       (run! (fn [[p v]] (set-param run-uuid comp id p v req)))))

(defn get-scale-state
  "Get scale state of the component instance."
  [run-uuid comp id]
  (get-param run-uuid comp id "scale.state"))

(defn get-state
  "Get state of the run."
  [run-uuid]
  (let [uri (ri/run-state-uri run-uuid)]
    (ri/get uri *config*)))

(defn get-abort
  "Get abort message."
  [run-uuid]
  (try
    (ri/get (ri/run-abort-uri run-uuid) *config*)
    (catch clojure.lang.ExceptionInfo e (ri/parse-ex-412 e))
    (catch java.util.concurrent.ExecutionException e (ri/parse-ex-412 (.getCause e)))))

(defn get-multiplicity
  "Get multiplicity of application component 'comp'."
  [run-uuid comp]
  (Integer/parseInt (get-param run-uuid comp nil "multiplicity")))

(defn get-comp-ids
  "Get list of instance IDs of application component 'comp'."
  [run-uuid comp]
  (remove #(zero? (count %))
          (-> (try
                (get-param run-uuid comp nil "ids")
                (catch clojure.lang.ExceptionInfo e (ri/parse-ex-412 e))
                (catch java.util.concurrent.ExecutionException e (ri/parse-ex-412 (.getCause e))))
              (u/split #",")
              (sort))))


;; Predicates.
(defn aborted?
  "Check if run is in 'Aborted' state."
  [run-uuid]
  (not (s/blank? (get-abort run-uuid))))

(defn scalable?
  "Check if run is scalable."
  [run-uuid]
  (-> (ri/get (ri/run-uri run-uuid) *config* ri/as-xml)
      xml/parse-str
      :attrs
      :mutable
      read-string))

(defn can-scale?
  "Check if it's possible to scale the run."
  [run-uuid]
  ; TODO: Use single call to get run representation and query it locally.
  ;       At the moment it's xml, which is not that comfortable to parse.
  (and
    (scalable? run-uuid)
    (not (aborted? run-uuid))
    (u/in? (get-state run-uuid) ri/scalable-states)))

(defn get-run-info
  [run-uuid]
  {:url       (ri/run-url (:service-url *config*) run-uuid)
   :state     (get-state run-uuid)
   :scalable  (scalable? run-uuid)
   :can-scale (can-scale? run-uuid)
   :aborted   (aborted? run-uuid)
   :abort-msg (get-abort run-uuid)})


;; Actions on the run.
(def wait-timeout-default 600)

(defn cancel-abort
  "Cancel abort on the run."
  [run-uuid]
  (ri/delete
    (ri/run-abort-uri run-uuid) *config*))

(defn terminate
  "Terminate the run."
  [run-uuid]
  (ri/delete
    (ri/run-uri run-uuid) *config*))

(defn scale-up
  "Scale up application component 'comp' by 'n' instances. Allow to set parameters
  from 'params' map on the new component instances. Returns list of added component
  instance names qualified with IDs."
  ([run-uuid comp n]
   (let [resp (ri/post
                (ri/to-component-uri run-uuid comp)
                {"n" n}
                *config*)]
     (u/split (:body resp) #",")))
  ([run-uuid comp n params]
   (let [added-instances (scale-up run-uuid comp n)
         ids             (ri/extract-ids added-instances)]
     (run! #(set-params run-uuid comp % params) ids)
     added-instances)))

(defn scale-down
  "Scale down application component 'comp' by terminating instances defined by
  'ids' vector."
  [run-uuid comp ids]
  (:body (ri/delete
           (ri/to-component-uri run-uuid comp)
           *config*
           {:body (str "ids=" (s/join "," ids))})))

(defn- wait-state
  "Waits for state 'state' for 'timeout' seconds using 'interval' seconds."
  [run-uuid state & [& {:keys [timeout interval]
                        :or   {timeout 60 interval 5}}]]
  (log/debug "Waiting for" state "state for" timeout "sec.")
  (u/wait-for #(= state (get-state run-uuid)) timeout interval))

(defn wait-ready
  "Waits for Ready state on the run. Returns true on success."
  ([run-uuid]
   (wait-state run-uuid "Ready" :timeout wait-timeout-default :interval 5))
  ([run-uuid timeout]
   (wait-state run-uuid "Ready" :timeout timeout :interval 5)))


;; Composite actions.
(def action-success "success")
(def action-failure "failure")

(defn- run-scale-action
  [run-uuid act comp n [& params]]
  (cond
    (= act "up") (scale-up run-uuid comp n params)
    ; FIXME: If run is aborted, server returns html - not json or xml.
    (= act "down") (if (aborted? run-uuid) (ri/throw-409) (scale-down run-uuid comp n))
    :else (throw (Exception. (str "Unknown scale action requested: " act)))))

(def action-result
  {:state        action-success
   :reason       nil
   :action       nil
   :comp-name    nil
   :multiplicity nil})

(defn- action-scale
  "Call scale action on 'comp' by action comp 'act'."
  [run-uuid act comp n & [& {:keys [params timeout]
                             :or   {params {} timeout wait-timeout-default}}]]
  (let [res (assoc action-result
              :action (format "scale-%s" act)
              :comp-name comp)]
    (log/debug (format "Scaling %s." act) comp n params)
    (try
      (let [ret (run-scale-action run-uuid act comp n params)
            _   (wait-ready timeout)]
        (log/debug (format "Success. Finished scaling %s." act) comp n params)
        (assoc res
          :reason ret
          :multiplicity (get-multiplicity run-uuid comp)))
      (catch Exception e
        (log/error (format "Failure scaling %s." act) comp n params e)
        (assoc res
          :state action-failure
          :reason (ri/reason-from-exc (ex-data e)))))))

(defn action-success?
  "Given the 'result' returned by an action, check if it was successfull."
  [result]
  (= action-success (:state result)))

(defn action-scale-up
  "Scale application component 'comp' by 'n' instances up.  Wait for the
  completion of the action. Optionally provide map of parameters as 'params'."
  [run-uuid comp n & [& {:keys [params timeout]
                         :or   {params {} timeout wait-timeout-default}}]]
  (action-scale run-uuid "up" comp n :params params :timeout timeout))

(defn- take-last-n-ids
  "Returns the last 'n' IDs of the currently running instances of application
  component 'comp'."
  [run-uuid comp n]
  (take-last n (get-comp-ids run-uuid comp)))

(defn action-scale-down-by
  "Scale down application component 'comp' by 'n' instances. Wait for
  the completion of the action."
  [run-uuid comp n & [& {:keys [timeout]
                         :or   {timeout wait-timeout-default}}]]
  (action-scale run-uuid "down" comp
                (take-last-n-ids run-uuid comp n)
                :timeout timeout))

(defn action-scale-down-at
  "Scale down application component 'comp' by terminating the component
  instances identified by IDs in 'ids'. Wait for the completion of the action."
  [run-uuid comp ids & [& {:keys [timeout]
                           :or   {timeout wait-timeout-default}}]]
  (action-scale run-uuid "down" comp ids :timeout timeout))

