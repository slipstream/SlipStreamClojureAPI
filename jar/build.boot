(def +version+ "3.1-SNAPSHOT")

(defn sixsq-repo [version edition]
  (let [nexus-url "http://nexus.sixsq.com/content/repositories/"
        repo-type (if (re-find #"SNAPSHOT" version)
                    "snapshots"
                    "releases")]
    (str nexus-url repo-type "-" edition)))

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamClientAPI-jar
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community")

(set-env!
  :source-paths #{"resources" "dev-resources" "test/clj" "test/cljc" "test/cljs"}
  :resource-paths #{"src/clj" "src/cljc" "src/cljs"}

  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-repo (get-env :version) (get-env :edition))}]])

  :dependencies
  '[[org.clojure/clojure "1.8.0" :scope "provided"]
    [org.clojure/clojurescript "1.8.40" :scope "provided"]
    [adzerk/boot-test "1.1.0" :scope "test"]
    [adzerk/boot-cljs "1.7.228-1" :scope "test"]
    [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
    [adzerk/boot-reload "0.4.5" :scope "test"]
    [tolitius/boot-check "0.1.1" :scope "test"]
    [sixsq/boot-deputil "0.2.2" :scope "test"]
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
  pom {:project (get-env :project)
       :version (get-env :version)}
  checkout {:dependencies [['sixsq/default-deps (get-env :version)]]}
  apidoc {:version     (get-env :version)
          :title       "SlipStream Client API"
          :sources     #{"src"}
          :description "Client library to interact with SlipStream via REST API."
          :target      "target/doc/api"}
  cljs {:optimizations :advanced}
  test-cljs {:js-env :phantom})

(deftask failed-test-cljs?
         "Raise exception on clojurescript test errors. Works around an issue
          with the provided crisptrutski.boot-cljs-test/exit! function that
          aborts the entire JVM with a System/exit!"
         []
         (fn [_]
           (fn [_]
             (when @crisptrutski.boot-cljs-test/failures?
               (throw (ex-info "ERROR: clojurescript test failures!" {}))))))

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (aot :all true)
           (test)
           (test-cljs)
           (failed-test-cljs?)))

(deftask build []
         (comp
           (pom)
           (jar)))

(deftask mvn-test
         "run all tests of project"
         []
         (run-tests))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
           (build)
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
