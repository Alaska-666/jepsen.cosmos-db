(ns jepsen.cosmosDB
  (:require [clojure.tools.logging :refer :all]
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

;(def cli-opts
;  "Additional command line options."
;  [["-w" "--workload NAME" "What workload should we run?"
;    :missing  (str "--workload " (cli/one-of workloads))
;    :validate [workloads (cli/one-of workloads)]]
;   ["-q" "--quorum" "Use quorum reads, instead of reading from any primary."]
;   ["-r" "--rate HZ" "Approximate number of requests per second, per thread."
;    :default  10
;    :parse-fn read-string
;    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
;   [nil "--ops-per-key NUM" "Maximum number of operations on any given key."
;    :default  100
;    :parse-fn client/parse-long
;    :validate [pos? "Must be a positive integer."]]])


(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (let [host    (String (:host opts))
        key    (String (:key opts))
        level (ConsistencyLevel (:level opts))]
  (merge tests/noop-test
         opts
         {:name            str "cosmos db consistency level=" level " "
          :os              debian/os
          :db              (db/db opts)
          :client          (Client. nil host key level)
          :pure-generators true})
  ))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn cosmosdb-test})
                   (cli/serve-cmd))
            args))