(ns sixsq.slipstream.client.api.defaults
  "Provides default values for client endpoints.")

(def cep-endpoint "https://nuv.la/api/cloud-entry-point")

;;
;; legacy endpoints
;;

(def login-endpoint "https://nuv.la/auth/login")

(def logout-endpoint "https://nuv.la/auth/logout")

(def modules-endpoint "https://nuv.la/module")              ;; must NOT end with a slash!

(def runs-endpoint "https://nuv.la/run")

