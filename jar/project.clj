(def +version+ "3.45-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamClientAPI-jar
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-doo "0.1.8"]
            [kirasystems/lein-codox "0.10.4"] 
            ;; FIXME update to lein-codox (remove kirasystems) after fix of 
            ;; https://github.com/sattvik/leinjacker/issues/14
            ;; (leinjacker 0.4.3 is published and lein-codox update their leinjacker dependency to 0.4.3) 
            [lein-shell "0.5.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src/clj" "src/cljc"]

  :clean-targets ^{:protect false} ["target" "out"]

  :aot [sixsq.slipstream.client.api.cimi
        sixsq.slipstream.client.api.authn]

  :codox {:name         "com.sixsq.slipstream/SlipStreamClientAPI-jar"
          :version      ~+version+
          :source-paths #{"src/clj" "src/cljc"}
          :source-uri   "https://github.com/slipstream/SlipStreamClojureAPI/blob/master/jar/{filepath}#L{line}"
          :language     :clojure
          :metadata     {:doc/format :markdown}}

  :doo {:verbose true
        :debug true
        :paths {:phantom "phantomjs --web-security=false"}}

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/clojurescript]
   [org.clojure/tools.logging]
   [log4j]
   [com.cemerick/url]
   [clojure-ini]
   [superstring]
   [org.clojure/data.json]
   [org.clojure/data.xml]
   [org.clojure/core.async]
   [io.nervous/kvlt]
   [com.taoensso/timbre]
   [org.json/json]]

  :cljsbuild {:builds [{:id           "test"
                        :source-paths ["test/cljc" "test/cljs"]
                        :compiler     {:main          'sixsq.slipstream.client.runner
                                       :output-to     "target/clienttest.js"
                                       :optimizations :none}}]}

  :profiles {:test {:aot            :all
                    :source-paths   ["test/clj" "test/cljc"]
                    :resource-paths ["dev-resources"]}}

  :aliases {"test"    ["do"
                       ["test"]
                       ["with-profiles" "test" ["doo" "phantom" "test" "once"]]]
            "docs"    ["codox"]
            "publish" ["shell" "../publish-docs.sh"]
            })
