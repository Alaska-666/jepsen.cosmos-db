(defproject cosmos-db-jepsen "0.1.0-SNAPSHOT"
  :description "A Jepsen test for Cosmos db"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main cosmos-db-jepsen
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [jepsen "0.2.1-SNAPSHOT"]
                 [com.azure/azure-cosmos "4.0.0-preview.1"]]
  :repl-options {:init-ns cosmos-db-jepsen.core})