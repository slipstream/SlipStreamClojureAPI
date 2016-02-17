(ns com.sixsq.slipstream.clj-client.run
  "The 'run' namespace contains functions for interacting with SlipStream run.

  It allows to
  * scale the components of the running application up and down,
  * query state of the run,
  * query and set runtime parameters (RTPs) of the application components and
    their instances,
  * query RTPs from global namespace (ss:),
  * terminate the run.

  Below is the terminology on examples.

  Application component: webapp
  Application component runtime parameter: webapp:ids
  Application component instance: webapp.1
  Application component instance runtime parameter: webapp.1:hostname

  RTP - runtime parameter. There are three types of RTPs
  * global RTP - ss:tags
  * application component RTP - webapp:ids
  * application component instance RTP - webapp.1:hostname

  Timeouts and intervals are in seconds.
  "
  (:require
    [com.sixsq.slipstream.clj-client.utils :as u]
    [com.sixsq.slipstream.clj-client.run-impl :as ri]
    [superstring.core :as s]
    [clojure.tools.logging :as log]
    [clj-http.client :as http]
    [clojure.data.xml :as xml])
  (:use [slingshot.slingshot :only [try+]]))


;;
;; Public library API.


;; Getters and setters of component, component instance and global RTPs.
(defn get-rtp
  "Get runtime parameter 'param' of application component instance 'name.id'.
  When 'id' is nil, gets runtime parameter of the application component as 'name:param'."
  [name id param]
  (:body
    (http/get
      (ri/build-rtp-url name id param)
      ri/rtp-req-params)))

(defn set-rtp
  "Set runtime parameter 'param' to 'value' on application component instance 'name.id'."
  [name id param value]
  (http/put
    (ri/build-rtp-url name id param)
    (merge ri/rtp-req-params {:body value})))

(defn get-scale-state
  "Get 'scale.state' paramter of component instance."
  [name id]
  (get-rtp name id "scale.state"))

(defn get-state
  "Get state of the run."
  []
  (:body
    (http/get
      ri/run-state-url
      ri/rtp-req-params)))

(defn get-abort
  "Get abort message."
  []
  (try+
    (:body (http/get ri/run-abort-url ri/rtp-req-params))
    (catch [:status 412] {} "")))

(defn get-multiplicity
  "Get multiplicity of application component 'name'."
  [name]
  (Integer/parseInt (get-rtp name nil "multiplicity")))

(defn get-instance-ids
  "Get list of instance IDs of application component 'name'."
  [name]
  (remove #(zero? (count %))
          (-> (try+
                (get-rtp name nil "ids")
                (catch [:status 412] {} ""))
              (u/split #",")
              (sort))))


;; Predicates.
(defn aborted?
  "Check if run is in \"Aborted\" state."
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
    ri/rtp-req-params))

(defn terminate
  "Terminate the run."
  []
  (http/delete
    ri/run-url
    ri/base-req-params))

(defn scale-up
  "Scale up application component 'name' by 'n' instances. Allow to set runtime
  parameters 'rtps' on the new component instances. Returns list of added component
  instance names qualified with IDs."
  ([name n]
   (u/split (:body (http/post
                     (ri/build-node-url name)
                     (merge ri/scale-req-params {:body (str "n=" n)})))
            #","))
  ([name n rtps]
   (let [added-instances (scale-up name n)]
     (doseq [[k v] rtps]
       (doseq [id (ri/extract-ids added-instances)]
         (set-rtp name id k v)))
     added-instances)))

(defn scale-down
  "Scale down application component 'name' by terminating instances defined by
  'ids' vector."
  [node-name ids]
  (:body (http/delete
           (ri/build-node-url node-name)
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
  [a node-name n [& rtps]]
  (cond
    (= a "up") (scale-up node-name n rtps)
    ; FIXME: If run is aborted, server returns html - not json or xml.
    (= a "down") (if (aborted?) (ri/throw-409) (scale-down node-name n))
    :else (throw (Exception. (str "Unknown scale action requested: " a)))))

(def action-result
  {:state        action-success
   :reason       nil
   :action       nil
   :node-name    nil
   :multiplicity nil})

(defn- action-scale
  "Call scale action by name."
  [a name n & [& {:keys [rtps timeout]
                  :or   {rtps {} timeout wait-timeout-default}}]]
  (let [res (assoc action-result
              :action (format "scale-%s" a)
              :node-name name)]
    (log/debug (format "Scaling %s." a) name n rtps)
    (try+
      (let [ret (run-scale-action a name n rtps)
            _   (wait-ready timeout)]
        (log/debug (format "Success. Finished scaling %s." a) name n rtps)
        (assoc res
          :reason ret
          :multiplicity (get-multiplicity name)))
      (catch Object o
        (log/error (format "Failure scaling %s." a) name n rtps o)
        (assoc res
          :state action-failure
          :reason (ri/reason-from-exc o))))))

(defn action-success?
  "Given the 'result' returned by an action, check if it was successfull."
  [result]
  (= action-success (:state result)))

(defn action-scale-up
  "Scale application component name by n instances up.  Wait for the
  completion of the action. Optionally provide map of RTPs as rtps.
  "
  [name n & [& {:keys [rtps timeout]
                :or   {rtps {} timeout wait-timeout-default}}]]
  (action-scale "up" name n :rtps rtps :timeout timeout))

(defn- take-last-n-ids
  "Returns the last 'n' IDs of the running application component instances."
  [name n]
  (take-last n (get-instance-ids name)))

(defn action-scale-down-by
  "Scale down application component 'name' by 'n' instances. Wait for
  the completion of the action."
  [name n & [& {:keys [timeout]
                :or   {timeout wait-timeout-default}}]]
  (action-scale "down" name
                (take-last-n-ids name n)
                :timeout timeout))

(defn action-scale-down-at
  "Scale down application component 'name' by terminating the component
  instances identified by IDs in 'ids'. Wait for the completion of the action.
  "
  [name ids & [& {:keys [timeout]
                  :or   {timeout wait-timeout-default}}]]
  (action-scale "down" name ids :timeout timeout))

