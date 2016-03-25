(def artifact-id 'com.sixsq.slipstream/SlipStreamClientAPI-jar)
(def artifact-version "3.1-SNAPSHOT")
(def repo-type (if (re-find #"SNAPSHOT" artifact-version) "snapshots" "releases"))
(def edition "community")
(def nexus-url "http://nexus.sixsq.com/content/repositories/")

(set-env!
  :source-paths #{"resources" "dev-resources" "test/clj" "test/cljc" "test/cljs"}
  :resource-paths #{"src/clj" "src/cljc" "src/cljs"}

  :repositories
  #(reduce conj %
           [["sixsq" {:url (str nexus-url repo-type "-" edition)}]])

  :dependencies
  '[[org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.8.34"]
    [adzerk/boot-test "1.1.0" :scope "test"]
    [adzerk/boot-cljs "1.7.228-1" :scope "test"]
    [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
    [adzerk/boot-reload "0.4.5" :scope "test"]
    [tolitius/boot-check "0.1.1" :scope "test"]
    [sixsq/boot-deputil "0.2.1" :scope "test"]
    [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
    [funcool/boot-codeina "0.1.0-SNAPSHOT" :scope "test"]])

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[sixsq.boot-deputil :refer [set-deps!]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]]
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
          :target      "target/doc/api"}
  test-cljs {:js-env :phantom
             :exit?  true})

(deftask build-clj
         "build full project"
         []
         (comp
           (pom)
           (test)
           (aot :all true)
           (jar)
           (install)
           (target)))

(deftask build-cljs
  "build clojurescript"
  []
  (comp
   (cljs :optimizations :advanced)
   (test-cljs)))

(deftask build []
  (comp (build-clj) (build-cljs)))

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
           (mvn-build)
           (push :repo "sixsq")))

(deftask setup-deps
         "setup dependencies for project"
         []
         (comp (checkout) (set-deps!)))

(boot (setup-deps))
