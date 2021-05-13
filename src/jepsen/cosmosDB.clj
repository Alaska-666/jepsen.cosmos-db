(ns jepsen.cosmosDB
  (:require [clojure.tools.logging :refer [info warn]]
            [clojure [string :as str]
             [pprint :refer [pprint]]]
            [jepsen [cli :as cli]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [jepsen.cosmosDB [db :as db]
             [listAppend :as listAppend]])
  (:import (jepsen.cosmosDB.listAppend Client))
  (:import (com.azure.cosmos ConsistencyLevel)))

;(defn -main
;  "Handles command line arguments. Can either run a test, or a web server for
;  browsing results."
;  [& args]
;  (cli/run! (merge (cli/single-test-cmd {:test-fn  mongodb-test
;                                         :opt-spec cli-opts})
;                   (cli/test-all-cmd {:tests-fn (partial all-tests mongodb-test)
;                                      :opt-spec cli-opts})
;                   (cli/serve-cmd))
;            args))


(def consistency-levels
  {:eventual  (ConsistencyLevel/EVENTUAL)
   :session   (ConsistencyLevel/SESSION)
   :staleness (ConsistencyLevel/BOUNDED_STALENESS)
   :strong    (ConsistencyLevel/STRONG)
   :prefix    (ConsistencyLevel/CONSISTENT_PREFIX)})

(def cli-opts
  "Additional command line options."
  [["-c" "--consistency LEVEL" "What level of consistency we should set: eventual, session, staleness, strong, prefix."
    :default :strong
    :parse-fn keyword
    :validate [#{:eventual
                 :session
                 :staleness
                 :strong
                 :prefix}
               "Should be one of eventual, session, staleness, strong, prefix."]]
   [nil "--key STRING" "Azure Cosmos DB account key." :default "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="]
   [nil "--host STRING" "Azure Cosmos DB account host." :default "https://localhost:8081"]])


(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (let [host              (:host opts)
        key               (:key opts)
        consistency-level (get consistency-levels (:consistency opts))]
  (merge tests/noop-test
         opts
         {:name            (str "cosmos db consistency level=" (:consistency opts) " ")
          :os              debian/os
          :db              (db/db opts)
          :client          (Client. host key consistency-level)
          :pure-generators true})
  )
  )


(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  cosmosdb-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args)
  )