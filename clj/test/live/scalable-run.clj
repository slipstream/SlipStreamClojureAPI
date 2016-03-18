#!/usr/bin/env lein exec -p
"
Prerequisites.

1. Define a single component SlipStream application and deploy it in a scalable mode.

2. Obtain the context file from a VM of the component that will be scaled

./test/live/get-context.sh <ip>

This creates ~/slipstream.context with the context from the VM.

Now you should be ready to proceed.

You can provide two optional parameters

./test/live/scalable-run.clj [component-name] [new-VM-size]

component-name - name of the application component. Default: testvm
new-VM-size    - VM size for the diagonal scaling test. Default: Tiny
"

; Name of the deployed component to be used for scaling.
(def comp-name (if (> (count *command-line-args*) 1)
                 (nth *command-line-args* 1)
                 "testvm"))
; Cloud releated instance type. Used below in diagonal scale up action.
(def test-instance-type (if (> (count *command-line-args*) 2)
                          (nth *command-line-args* 2)
                          "Tiny"))

; Loading the namespace should find and read ~/slipstream.context
(require '[com.sixsq.slipstream.clj-client.run :as r] :reload)
(use '[clojure.pprint :only [pprint]])

(defn print-run
  []
  (pprint (r/get-run-info)))

(defn action [& msg]
  (apply println ":::\n:::" msg))

(defn step [& msg]
  (apply println "   -" msg))

(defn error [& msg]
  (apply println "ERROR:" msg)
  (print-run)
  ((System/exit 0)))

(defn wait-ready-or-error
  []
  (step "Waiting for Ready state.")
  (if-not (true? (r/wait-ready))
    (error "Failed waiting for the run to enter Ready state.")))

(defn check-scalable
  []
  (step "Check if run is scalable.")
  (if-not (true? (r/scalable?))
    (error "Run is not scalable.")))

(defn check-multiplicity
  [exp]
  (if-not (= exp (r/get-multiplicity comp-name))
    (error (format "Multiplicity should be %s." exp))))

(defn check-instance-ids
  [exp]
  (let [exp-str (map str exp)]
    (if-not (= exp-str (r/get-comp-ids comp-name))
      (error (format "Instance IDs should be %s." exp-str)))))

(defn check-can-scale
  []
  (if-not (true? (r/can-scale?))
    (error "Should be able to scale at this stage.")))

(defn check-cannot-scale
  []
  (if-not (false? (r/can-scale?))
    (error "Should NOT be able to scale at this stage.")))

(defn inst-names-range
  [start stop]
  (vec (map #(str comp-name "." %) (range start stop))))

(defn prn-inst-scale-states
  [ids]
  (println (map
             #(list (str comp-name "." %) (r/get-scale-state comp-name %))
             ids)))


;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

(step "Live test of the SlipStream clojure API library via scaling SlipStream run.")
(step (format "Component to scale: '%s'" comp-name))
(step (format "VM instance type for diagonal scaling: '%s'" test-instance-type))

(action "Run:")
(print-run)

;;
;; Actions and assertions.
;;
(action "Starting tests.")
(wait-ready-or-error)
(check-scalable)
(check-multiplicity 1)
(check-instance-ids '(1))
(check-can-scale)
(prn-inst-scale-states '(1))

(action "Artificially abort the run and then recover from the abort.")
(try
  (r/get-param "foo" 0 "bar")
  (catch java.util.concurrent.ExecutionException e (let [status (:status (ex-data (.getCause e)))]
                                                     (if-not (= 404 status) (error "Unexpected HTTP error status:" status))))
  (catch Exception e (error "Unexpected error:" (ex-data e))))
(check-cannot-scale)
(if-not (true? (r/aborted?))
  (error "Run should be aborted at this stage."))
(if-not (= "Unknown key foo.0:bar" (r/get-abort))
  (error "Unexpected abort message."))
; FIXME: run/cancel-abort should return run/action-result map
(if-not (= 204 (:status (r/cancel-abort)))
  (error "Failed cancelling the abort flag."))
(check-can-scale)


(action "Scale up. Manual wait.")
(let [exp-inst-names (inst-names-range 2 4)
      inst-names     (r/scale-up comp-name 2)]
  (if-not (= exp-inst-names inst-names)
    (error "Expected to start" exp-inst-names ", but started" inst-names)))
(wait-ready-or-error)
(check-multiplicity 3)
(check-instance-ids '(1 2 3))
(check-can-scale)


(action "Scale down by IDs. Manual wait.")
(if-not (nil? (r/scale-down comp-name '(3 1)))
  (error "Scale down should have returned nil."))
(wait-ready-or-error)
(check-multiplicity 1)
(check-instance-ids '(2))
(check-can-scale)


(action "Diagonal scale up action (with internal wait). Providing VM size RTPs.")
(def cloudservice (r/get-param comp-name 1 "cloudservice"))
(def key-instance-type (str cloudservice ".instance.type"))
(let [res (r/action-scale-up comp-name 2
                             :rtps {key-instance-type test-instance-type}
                             :timeout 1200)]
  (if-not (and (r/action-success? res) (= (inst-names-range 4 6) (:reason res)))
    (error "Diagonal scale up failed:" res)))
(check-multiplicity 3)
(check-instance-ids '(2 4 5))
(step "'component id' => 'instance size'")
; TODO: add asserts for IDs 4 and 5
(doseq [id (r/get-comp-ids comp-name)]
  (step (format "%s => %s"
                id (r/get-param comp-name id key-instance-type))))
(check-can-scale)


(def inst-down '(2 4))
(action "Scale down action (with internal wait). Remove instances by ids:" inst-down)
(let [res (r/action-scale-down-at comp-name inst-down :timeout 1200)]
  (if-not (r/action-success? res)
    (error "Failed scaling down:" res)))
(check-multiplicity 1)
(check-instance-ids '(5))
(check-can-scale)


(action "Scale down action (with internal wait). Remove a number of instances.")
(let [res (r/action-scale-down-by comp-name 1 :timeout 1200)]
  (if-not (r/action-success? res)
    (error "Failed scaling down:" res)))
(check-multiplicity 0)
(check-instance-ids '())
(check-can-scale)


(action "Termintating run.")
; FIXME: run/terminate should return run/action-result map
(let [res (r/terminate)]
  (if-not (= 204 (:status res))
    (error "Failed to properly terminate the run:" res)))


(action "Validation.")
(let [res (r/get-state)]
  (if-not (= "Done" res)
    (error "Expected the run in Done state. Found:" res)))
(check-cannot-scale)

(action "Test finished successfully.")

