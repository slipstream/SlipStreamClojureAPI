(defproject com.sixsq.slipstream/clj-client "2.23-SNAPSHOT"
  :description "Clojure SlipStream Client"
  :url "http://sixsq.com"

  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [org.clojure/tools.logging "0.3.0"]
                 [log4j/log4j "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]
                 [clj-http "2.0.0"]
                 [clojure-ini "0.0.2"]
                 [slingshot "0.12.2"]
                 [superstring "2.1.0"]
                 [clj-json "0.5.3"]
                 [org.clojure/data.xml "0.0.8"]
                 ;;Environment settings
                 ;[environ                                   "1.0.0"]
                 ]

  :resource-paths ["src/resources" "test/resources"]

  :plugins [[lein-environ "1.0.0"]
            [lein-kibit "0.1.2"]]

  :jar-name "clj-ss-client-%s.jar"
  :uberjar-name "clj-ss-client-%s-standalone.jar")