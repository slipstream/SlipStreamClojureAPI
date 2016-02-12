; Start SlipStream deployment.
; Get context file from VM of the type that will be scaled:
; ssh -l root <ip> cat /opt/slipstream/client/sbin/slipstream.context > ~/slipstream.context

(require '[com.sixsq.slipstream.clj-client.run :as r] :reload)

(r/wait-ready 1200)

; Queries.
(r/get-state)

(r/scalable?)

; Set to the name of the node used for scaling.
(def node-name "testvm")
; Cloud releated instance type. Used below in diagonal scale up action.
(def test-instance-type "Tiny")

(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)

(r/can-scale?)

; Artificially abort the run and then recover from the abort.
(r/get-rtp "foo" 0 "bar")
(r/can-scale?)
(r/aborted?)
(r/get-abort)
(r/cancel-abort)

(r/can-scale?)

; Scale up. No wait.
(r/scale-up node-name 1)
(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)

(r/can-scale?)

; Scale up. Manual wait.
(r/scale-up node-name 3)
(r/wait-ready 900)
(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)

; Scale down by IDs
(r/scale-down node-name '(4 1))
(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)

; Scale up action. Provide RTP. Use internal wait.
(def cloudservice (r/get-rtp node-name 1 "cloudservice"))
(def key-instance-type (format "%s.instance.type" cloudservice))
(r/action-scale-up node-name 2
                   :rtps {key-instance-type test-instance-type}
                   :timeout-s 1200)
(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)
(doseq [id (r/get-node-ids node-name)]
  (println (format "%s = %s"
                   id (r/get-rtp node-name id key-instance-type))))

; Scale down action. Use internal wait.
(r/action-scale-down-by node-name 3 :timeout-s 1200)
(r/get-node-multiplicity node-name)
(r/get-node-ids node-name)

; Termination.
(r/terminate)

; Validation.
(r/get-state)
(r/can-scale?)
