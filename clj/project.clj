(defproject com.sixsq.slipstream/clj-client "2.24-SNAPSHOT"
  :description "Clojure SlipStream Client"
  :url "http://sixsq.com"

  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [io.nervous/kvlt "0.1.0"]

                 [org.clojure/tools.logging "0.3.0"]
                 [log4j/log4j "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]
                 [clojure-ini "0.0.2"]
                 [superstring "2.1.0"]
                 [clj-json "0.5.3"]
                 [org.clojure/data.xml "0.0.8"]]

  :resource-paths ["src/resources" "test/resources"]

  :plugins [[lein-environ "1.0.0"]
            [lein-kibit "0.1.2"]
            [lein-codox "0.9.4"]
            [lein-exec "0.3.6"]]

  :jar-name "clj-ss-client-%s.jar"
  :uberjar-name "clj-ss-client-%s-standalone.jar")
