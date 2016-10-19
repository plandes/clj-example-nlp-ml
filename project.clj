(defproject com.zensols.example/nlp-ml-example "0.1.0-SNAPSHOT"
  :description "Example Project for Natural Language Processing and Machine Learning Libraries"
  :url "https://github.com/plandes/clj-example-nlp-ml"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.9.5"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Cookbook for Zensols NLP and ML libraries"}
          :output-path "target/doc/codox"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :exclusions [org.slf4j/slf4j-log4j12
               ch.qos.logback/logback-classic]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.apache.logging.log4j/log4j-api "2.3"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.3"]
                 [org.apache.logging.log4j/log4j-jcl "2.3"]
                 [org.clojure/tools.logging "0.3.1"]

                 ;; command line
                 ;[com.zensols.tools/actioncli "0.0.9"]

                 ;; nlp/ml
                 [com.zensols.nlp/parse "0.0.10"]
                 [com.zensols.ml/dataset "0.0.6"]
                 [com.zensols.ml/model "0.0.6"]

                 ;; spreadsheet
                 ;[com.zensols.tools/misc "0.0.1"]

                 ;; dev
                 ;[com.zensols.gui/tabres "0.0.5"]
                 ]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.example.core"]
                                     [:id "saclassify"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:uberjar {:aot [zensols.example.core]}
             :appassem {:aot :all}
             :dev
             {:jvm-opts
              ["-Dlog4j.configurationFile=test-resources/log4j2.xml" "-Xms4g" "-Xmx12g" "-XX:+UseConcMarkSweepGC"]
              :dependencies [[nz.ac.waikato.cms.weka/weka-stable "3.6.12" :classifier "sources"]
                             [com.zensols/clj-append "1.0.4"]]}}
  :main zensols.example.core)
