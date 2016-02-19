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
    [clj-http.client :as http]
    [clojure.data.xml :as xml])
  (:use [slingshot.slingshot :only [try+]]
        [clojure.walk :only [stringify-keys]]))


;;
;; Public library API.


;; Getters and setters of component, component instance and global parameters.
(defn get-param
  "Get parameter 'param' of application component instance 'comp.id' as 'comp.id:param'.
  When 'id' is nil, gets parameter of the application component as 'comp:param'."
  [comp id param]
  (:body
    (http/get
      (ri/build-param-url comp id param)
      ri/param-req-params)))

(defn set-param
  "Set parameter 'param' to 'value' on application component instance 'comp.id'."
  [comp id param value]
  (http/put
    (ri/build-param-url comp id param)
    (merge ri/param-req-params {:body value})))

(defn set-params
  "Given a map of parameters 'params', set them on application component
  instance 'comp.id'."
  [comp id params]
  (->> params
       stringify-keys
       (run! (fn [[p v]] (set-param comp id p v)))))

(defn get-scale-state
  "Get scale state of the component instance."
  [comp id]
  (get-param comp id "scale.state"))

(defn get-state
  "Get state of the run."
  []
  (:body
    (http/get
      ri/run-state-url
      ri/param-req-params)))

(defn get-abort
  "Get abort message."
  []
  (try+
    (:body (http/get ri/run-abort-url ri/param-req-params))
    (catch [:status 412] {} "")))

(defn get-multiplicity
  "Get multiplicity of application component 'comp'."
  [comp]
  (Integer/parseInt (get-param comp nil "multiplicity")))

(defn get-comp-ids
  "Get list of instance IDs of application component 'comp'."
  [comp]
  (remove #(zero? (count %))
          (-> (try+
                (get-param comp nil "ids")
                (catch [:status 412] {} ""))
              (u/split #",")
              (sort))))


;; Predicates.
(defn aborted?
  "Check if run is in 'Aborted' state."
  []
  (not (s/blank? (get-abort))))

(defn scalable?
  "Check if run is scalable."
  []
  (-> (http/get ri/run-url ri/scale-req-params)
      :body
      xml/parse-str
      :attrs
      :mutable
      read-string))

(defn can-scale?
  "Check if it's possible to scale the run."
  []
  ; TODO: Use single call to get run representation and query it locally.
  ;       At the moment it's xml, which is not that comfortable to parse.
  (and
    (scalable?)
    (not (aborted?))
    (u/in? (get-state) ri/scalable-states)))

(defn get-run-info
  []
  {:url       ri/run-url
   :state     (get-state)
   :scalable  (scalable?)
   :can-scale (can-scale?)
   :aborted   (aborted?)
   :abort-msg (get-abort)})


;; Actions on the run.
(def wait-timeout-default 600)

(defn cancel-abort
  "Cancel abort on the run."
  []
  (http/delete
    ri/run-abort-url
    ri/param-req-params))

(defn terminate
  "Terminate the run."
  []
  (http/delete
    ri/run-url
    ri/base-http-params))

(defn scale-up
  "Scale up application component 'comp' by 'n' instances. Allow to set parameters
  from 'params' map on the new component instances. Returns list of added component
  instance names qualified with IDs."
  ([comp n]
   (u/split (:body (http/post
                     (ri/build-component-url comp)
                     (merge ri/scale-req-params {:body (str "n=" n)})))
            #","))
  ([comp n params]
   (let [added-instances (scale-up comp n)
         ids             (ri/extract-ids added-instances)]
     (run! #(set-params comp % params) ids)
     added-instances)))

(defn scale-down
  "Scale down application component 'comp' by terminating instances defined by
  'ids' vector."
  [comp ids]
  (:body (http/delete
           (ri/build-component-url comp)
           (merge ri/scale-req-params {:body (str "ids=" (s/join "," ids))}))))

(defn- wait-state
  "Waits for state 'state' for 'timeout' seconds using 'interval' seconds."
  [state & [& {:keys [timeout interval]
               :or   {timeout 60 interval 5}}]]
  (log/debug "Waiting for" state "state for" timeout "sec.")
  (u/wait-for #(= state (get-state)) timeout interval))

(defn wait-ready
  "Waits for Ready state on the run. Returns true on success."
  ([]
   (wait-state "Ready" :timeout wait-timeout-default :interval 5))
  ([timeout]
   (wait-state "Ready" :timeout timeout :interval 5)))


;; Composite actions.
(def action-success "success")
(def action-failure "failure")

(defn- run-scale-action
  [act comp n [& params]]
  (cond
    (= act "up") (scale-up comp n params)
    ; FIXME: If run is aborted, server returns html - not json or xml.
    (= act "down") (if (aborted?) (ri/throw-409) (scale-down comp n))
    :else (throw (Exception. (str "Unknown scale action requested: " act)))))

(def action-result
  {:state        action-success
   :reason       nil
   :action       nil
   :comp-name    nil
   :multiplicity nil})

(defn- action-scale
  "Call scale action on 'comp' by action comp 'act'."
  [act comp n & [& {:keys [params timeout]
                    :or   {params {} timeout wait-timeout-default}}]]
  (let [res (assoc action-result
              :action (format "scale-%s" act)
              :comp-name comp)]
    (log/debug (format "Scaling %s." act) comp n params)
    (try+
      (let [ret (run-scale-action act comp n params)
            _   (wait-ready timeout)]
        (log/debug (format "Success. Finished scaling %s." act) comp n params)
        (assoc res
          :reason ret
          :multiplicity (get-multiplicity comp)))
      (catch Object o
        (log/error (format "Failure scaling %s." act) comp n params o)
        (assoc res
          :state action-failure
          :reason (ri/reason-from-exc o))))))

(defn action-success?
  "Given the 'result' returned by an action, check if it was successfull."
  [result]
  (= action-success (:state result)))

(defn action-scale-up
  "Scale application component 'comp' by 'n' instances up.  Wait for the
  completion of the action. Optionally provide map of parameters as 'params'."
  [comp n & [& {:keys [params timeout]
                :or   {params {} timeout wait-timeout-default}}]]
  (action-scale "up" comp n :params params :timeout timeout))

(defn- take-last-n-ids
  "Returns the last 'n' IDs of the currently running instances of application
  component 'comp'."
  [comp n]
  (take-last n (get-comp-ids comp)))

(defn action-scale-down-by
  "Scale down application component 'comp' by 'n' instances. Wait for
  the completion of the action."
  [comp n & [& {:keys [timeout]
                :or   {timeout wait-timeout-default}}]]
  (action-scale "down" comp
                (take-last-n-ids comp n)
                :timeout timeout))

(defn action-scale-down-at
  "Scale down application component 'comp' by terminating the component
  instances identified by IDs in 'ids'. Wait for the completion of the action."
  [comp ids & [& {:keys [timeout]
                  :or   {timeout wait-timeout-default}}]]
  (action-scale "down" comp ids :timeout timeout))

