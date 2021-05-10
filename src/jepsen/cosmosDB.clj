(ns jepsen.cosmosDB
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]
            [jepsen.os.debian :as debian]
            [jepsen.cosmosDB [db :as db]])
  (:import (jepsen.cosmosDB.listAppend Client)))

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


(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:name            "cosmos db"
          :os              debian/os
          :db              (db/db opts)
          :client          (Client. nil)
          :pure-generators true}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn cosmosdb-test})
                   (cli/serve-cmd))
            args))