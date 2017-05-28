(def +version+ "3.29-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamClientAPI-jar
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url]])

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
                   [com.cemerick/url]
                   [clojure-ini]
                   [superstring]
                   [org.clojure/data.json]
                   [org.clojure/data.xml]
                   [org.clojure/core.async]
                   [io.nervous/kvlt "0.1.4"]
                   [com.taoensso/timbre]

                   [org.json/json "20160810"]

                   [doo]
                   [adzerk/boot-test]
                   [adzerk/boot-cljs]
                   [adzerk/boot-cljs-repl]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]
                   [crisptrutski/boot-cljs-test]
                   [onetom/boot-lein-generate]
                   [boot-codox]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs fs-snapshot fs-restore]]
  '[boot.lein :refer [generate]]
  '[codox.boot :refer [codox]])

(set-env!
  :source-paths #{"dev-resources" "test/clj" "test/cljc"}
  :resource-paths #{"src/clj" "src/cljc" "src/cljs"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  checkout {:dependencies [['sixsq/default-deps (get-env :version)]]}
  codox {:name         (str (get-env :project))
         :version      (get-env :version)
         :source-paths #{"src/clj" "src/cljc" "src/cljs"}
         :source-uri   "https://github.com/slipstream/SlipStreamClojureAPI/blob/master/jar/{filepath}#L{line}"
         :language     :clojure
         :metadata     {:doc/format :markdown}}
  cljs {:optimizations :advanced
        :compiler-options {:language-in :ecmascript5}}
  test-cljs {:js-env :phantom
             :doo-opts {:paths {:phantom "phantomjs --web-security=false"}}
             :cljs-opts {:language-in :ecmascript5}
             :exit? true}
  test {:junit-output-to ""}
  push {:repo "sixsq"})

(deftask test-compile
         "compile all files, discarding changes to fileset"
         []
         (comp
           (fs-snapshot)
           (aot :all true)
           (fs-restore)))

(deftask test-all
         "performs full compilation then runs all tests"
         []
         (comp
           (test-compile)
           (test)
           (test-cljs)))

(deftask build []
         (comp
           (pom)
           (jar)))

(deftask mvn-test
         "run all tests of project"
         []
         (test-all))

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
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))
