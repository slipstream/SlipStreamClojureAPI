(def +version+ "3.18-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamClientAPI-jar
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url
                                   lein-generate]])

(set-env!
  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-nexus-url)}]])

  :dependencies
  #(vec (concat %
                (merge-defaults
                 ['sixsq/default-deps (get-env :version)]
                 '[[org.clojure/clojure]
                   [org.clojure/clojurescript]
                   
                   [org.clojure/tools.logging]
                   [log4j]
                   [clojure-ini]
                   [superstring]
                   [org.clojure/data.json]
                   [org.clojure/data.xml]
                   [org.clojure/core.async]
                   [io.nervous/kvlt]
                   [com.taoensso/timbre]

                   [adzerk/boot-test]
                   [adzerk/boot-cljs]
                   [adzerk/boot-cljs-repl]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]
                   [org.clojars.sixsq/boot-cljs-test] ;; non-canonical fork
                   [boot-codox]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]]
  '[codox.boot :refer [codox]])

(set-env!
  :source-paths #{"dev-resources" "test/clj" "test/cljc"}
  :resource-paths #{"src/clj" "src/cljc"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  checkout {:dependencies [['sixsq/default-deps (get-env :version)]]}
  codox {:name         (str (get-env :project))
         :version      (get-env :version)
         :source-paths #{"src/clj" "src/cljc" "src/cljs"}
         :source-uri   "https://github.com/slipstream/SlipStreamClientAPI/blob/master/jar/{filepath}#L{line}"
         :language     :clojure
         :metadata     {:doc/format :markdown}}
  cljs {:optimizations :advanced}
  test-cljs {:js-env :phantom
             :doo-opts {:paths {:phantom "phantomjs --web-security=false"}}}
  test {:junit-output-to ""}
  )

(deftask failed-test-cljs?
         "Raise exception on clojurescript test errors. Works around an issue
          with the provided crisptrutski.boot-cljs-test/exit! function that
          aborts the entire JVM with a System/exit!"
         []
         (fn [_]
           (fn [_]
             (when @crisptrutski.boot-cljs-test/failures?
               (throw (ex-info "ERROR: clojurescript test failures!" {}))))))

(deftask run-cljs-tests
         "run only the clojurescript tests"
         []
         (comp
           (test-cljs)
           (failed-test-cljs?)))

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

(deftask docs
         "builds API documentation and puts into target"
         []
         (comp
           (codox)
           (sift :include #{#"^doc.*"})
           (target)))

(deftask publish
         "publish API documentation to GitHub pages branch"
         []
         (fn middleware [next-handler]
           (fn handler [fileset]
             (require 'clojure.java.shell)
             (let [sh (resolve 'clojure.java.shell/sh)
                   result (sh "../publish-docs.sh")]
               (if (zero? (:exit result))
                 (next-handler fileset)
                 (throw (ex-info "Publishing docs failed!" result)))))))

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
