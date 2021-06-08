(defproject jepsen.cosmosDB "0.1.0-SNAPSHOT"
  :description "A Jepsen test for Cosmos db"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main jepsen.cosmosDB
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.2.1-SNAPSHOT"]
                 [org.json/json "20210307"]
                 [com.azure/azure-cosmos "4.8.0"]]
  :jvm-opts ["-Djava.awt.headless=true"]
  :java-source-paths ["src/java/"]
  :source-paths      ["src/clojure"]
  :repl-options {:init-ns jepsen.cosmosDB})