(def +version+ "3.44")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamClientAPI-uber
  "3.44"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-localrepo "0.5.4"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.44"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :dependencies
  [[com.sixsq.slipstream/SlipStreamClientAPI-jar]]

  :profile {:provided {:dependencies [[org.clojure/clojure]]}}

  :exclude-scope #{"provided"}
  :uberjar-exclusions [#".*/pom.xml"
                       #"META-INF/.*\.SF"
                       #"META-INF/.*\.DSA"
                       #"META-INF/.*\.RSA"]

  :aliases {"install" [["do"
                        ["uberjar"]
                        ["pom"]
                        ["localrepo" "install" "-p" "target/pom.xml"
                         ~(str "target/SlipStreamClientAPI-uber-" +version+ "-standalone.jar")
                         "com.sixsq.slipstream/SlipStreamClientAPI-uber"
                         ~+version+]
                        ]]})
