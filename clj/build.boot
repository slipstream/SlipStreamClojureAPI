(def project-version "3.1-SNAPSHOT")

(set-env!
 :source-paths #{"resources" "test"}
 :resource-paths #{"src"}

 :repositories
 #(reduce conj %
          '[["boot-releases" {:url "http://nexus.sixsq.com/content/repositories/releases-boot"}]
            ["sixsq-snapshots" {:url "http://nexus.sixsq.com/content/repositories/snapshots-community"}]])

 :dependencies
 '[
   [org.clojure/clojure "1.8.0"]
   [environ "1.0.2"]

   [adzerk/boot-test "1.1.0" :scope "test"]
   [tolitius/boot-check "0.1.1" :scope "test"]
   [boot-environ "1.0.2" :scope "test"]
   [sixsq/boot-deputil "0.1.0" :scope "test"]
   [funcool/boot-codeina "0.1.0-SNAPSHOT" :scope "test"]
   ])

(require
 '[adzerk.boot-test :refer [test]]
 '[environ.boot :refer [environ]]
 '[sixsq.boot-deputil :refer [set-deps!]]
 '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
 '[funcool.boot-codeina :refer :all]
 )

(task-options!
 pom {:project 'com.sixsq.slipstream/SlipStreamClientAPI
      :version project-version}
 checkout {:dependencies [['sixsq/default-deps project-version]]}
 uber {:exclude-scope #{"provided"}
       :exclude #{#".*/pom.xml"
                  #"META-INF/.*\.SF"
                  #"META-INF/.*\.DSA"
                  #"META-INF/.*\.RSA"}}
 apidoc {:version project-version
         :title "SlipStream Client API"
         :sources #{"src"}
         :description "SlipStream Client library to interact with SlipStream via REST API."
         :target "target/doc/api"})

(deftask build
  "build full project"
  []
  (comp
   (pom)
   (environ :env {:clj-env     "test"
                  :config-path "config-hsqldb-mem.edn"
                  :passphrase  "sl1pstre8m"})
   (test)
   (aot :all true)
   (jar)
   (install)
   (target)))

(deftask mvn-test
  "run all tests of project"
  []
  (comp
   (pom)
   (aot :all true)
   (test)))

(deftask mvn-build
  "build full project through maven"
  []
  (comp
   (pom)
   (uber)
   (jar)
   (install)
   (target)))

(deftask setup-deps
  "setup dependencies for project"
  []
  (comp (checkout) (set-deps!)))

(boot (setup-deps))
