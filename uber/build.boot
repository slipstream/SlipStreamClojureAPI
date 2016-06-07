(def +version+ "3.6-SNAPSHOT")

(defn sixsq-repo [version edition]
  (let [nexus-url "http://nexus.sixsq.com/content/repositories/"
        repo-type (if (re-find #"SNAPSHOT" version)
                    "snapshots"
                    "releases")]
    (str nexus-url repo-type "-" edition)))

(set-env!
 :project 'com.sixsq.slipstream/SlipStreamClientAPI-uber
 :version +version+
 :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
 :edition "community")

(set-env!
 :source-paths #{"resources"}
 
 :repositories
 #(reduce conj % [["sixsq" {:url (sixsq-repo (get-env :version) (get-env :edition))}]])

 :dependencies
 '[[org.clojure/clojure "1.8.0" :scope "provided"]
   [sixsq/boot-deputil "0.2.2" :scope "test"]])

(require
 '[sixsq.boot-deputil :refer [set-deps!]])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  checkout {:dependencies [['sixsq/default-deps (get-env :version)]]}
  uber {:exclude-scope #{"provided"}
        :exclude       #{#".*/pom.xml"
                         #"META-INF/.*\.SF"
                         #"META-INF/.*\.DSA"
                         #"META-INF/.*\.RSA"}})

 (deftask build []
   (comp
    (pom)
    (uber)
    (jar)))

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
