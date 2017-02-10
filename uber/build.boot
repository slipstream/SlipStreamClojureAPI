(def +version+ "3.21")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamClientAPI-uber
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
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
                 '[[org.clojure/clojure nil :scope "provided"]
                   [com.sixsq.slipstream/SlipStreamClientAPI-jar]]))))

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  uber {:exclude-scope #{"provided"}
        :exclude       #{#".*/pom.xml"
                         #"META-INF/.*\.SF"
                         #"META-INF/.*\.DSA"
                         #"META-INF/.*\.RSA"}}
  push {:repo "sixsq"})

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
    (if (= "true" (System/getenv "BOOT_PUSH"))
      (push)
      identity)))
