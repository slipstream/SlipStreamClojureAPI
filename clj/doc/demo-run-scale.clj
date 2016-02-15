"Prerequisites.

1. Define a single component SlipStream application and deploy it in a scalable mode.

2. Obtain the context file from a VM of the component that will be scaled
ssh -l root <ip> cat /opt/slipstream/client/sbin/slipstream.context > ~/slipstream.context

3. The functions in the demo are intended to be manully run in REPL.
For examaple, go to clj/ directory of the project and start REPL with
 lein repl

Now you should be ready to proceed.
"

; Loading the namespace should find and read ~/slipstream.context
(require '[com.sixsq.slipstream.clj-client.run :as r] :reload)

; Wait in case the deployment is still provisining.
(r/wait-ready 1200)

; Queries.
(r/get-state)

(r/scalable?)

; Define to the name of the deployed component to be used for scaling.
(def comp-name "testvm")
; Cloud releated instance type. Used below in diagonal scale up action.
(def test-instance-type "Tiny")

(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

(r/can-scale?)

; Artificially abort the run and then recover from the abort.
(r/get-rtp "foo" 0 "bar")
(r/can-scale?)
(r/aborted?)
(r/get-abort)
(r/cancel-abort)

(r/can-scale?)

; Scale up. No wait.
(r/scale-up comp-name 1)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

(r/can-scale?)

; Scale up. Manual wait.
(r/scale-up comp-name 3)
(r/wait-ready 900)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

; Scale down by IDs. Manual wait.
(r/scale-down comp-name '(4 1))
(r/wait-ready 900)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

; Scale up action. Provide RTP. Use internal wait.
(def cloudservice (r/get-rtp comp-name 1 "cloudservice"))
(def key-instance-type (str cloudservice ".instance.type"))
(r/action-scale-up comp-name 2
                   :rtps {key-instance-type test-instance-type}
                   :timeout 1200)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)
(doseq [id (r/get-instance-ids comp-name)]
  (println (format "%s = %s"
                   id (r/get-rtp comp-name id key-instance-type))))

; Scale down action. Remove instances 2, 3 and 6.  Use internal wait.
(r/action-scale-down-at comp-name '(2 3 6) :timeout 1200)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

; Scale down action. Remove to instances. Use internal wait.
(r/action-scale-down-by comp-name 2 :timeout 1200)
(r/get-multiplicity comp-name)
(r/get-instance-ids comp-name)

; Terminate run.
(r/terminate)

; Validation.
(r/get-state)
(r/can-scale?)
