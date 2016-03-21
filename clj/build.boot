(def artifact-id 'com.sixsq.slipstream/SlipStreamClientAPI-jar)
(def artifact-version "3.1-SNAPSHOT")
(def repo-type (if (re-find #"SNAPSHOT" artifact-version) "snapshots" "releases"))
(def edition "community")
(def nexus-url "http://nexus.sixsq.com/content/repositories/")

(set-env!
  :source-paths #{"resources" "test"}
  :resource-paths #{"src"}

  :repositories
  #(reduce conj %
           [["boot-releases" {:url (str nexus-url "releases-boot")}]
            ["sixsq" {:url (str nexus-url repo-type "-" edition)}]])

  :dependencies
  '[[org.clojure/clojure "1.8.0"]
    [adzerk/boot-test "1.1.0" :scope "test"]
    [tolitius/boot-check "0.1.1" :scope "test"]
    [sixsq/boot-deputil "0.1.0" :scope "test"]
    [funcool/boot-codeina "0.1.0-SNAPSHOT" :scope "test"]])

(require
  '[adzerk.boot-test :refer [test]]
  '[sixsq.boot-deputil :refer [set-deps!]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[funcool.boot-codeina :refer [apidoc]])

(task-options!
  pom {:project artifact-id
       :version artifact-version}
  checkout {:dependencies [['sixsq/default-deps artifact-version]]}
  uber {:exclude-scope #{"provided"}
        :exclude       #{#".*/pom.xml"
                         #"META-INF/.*\.SF"
                         #"META-INF/.*\.DSA"
                         #"META-INF/.*\.RSA"}}
  apidoc {:version     artifact-version
          :title       "SlipStream Client API"
          :sources     #{"src"}
          :description "Client library to interact with SlipStream via REST API."
          :target      "target/doc/api"})

(deftask build
         "build full project"
         []
         (comp
           (pom)
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

(deftask mvn-deploy
         "build full project through maven"
         []
         (comp
           (pom)
           (uber)
           (jar)
           (install)
           (target)
           (push :repo "sixsq")))

(deftask setup-deps
         "setup dependencies for project"
         []
         (comp (checkout) (set-deps!)))

(boot (setup-deps))
