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
  "A map of consistency levels names to functions that construct ConsistencyLevel, given opts."
  {:eventual ConsistencyLevel/EVENTUAL
   :session ConsistencyLevel/SESSION
   :staleness ConsistencyLevel/BOUNDED_STALENESS
   :strong ConsistencyLevel/STRONG
   :prefix ConsistencyLevel/CONSISTENT_PREFIX
   })

(def cli-opts
  "Additional command line options."
  [["-k" "--key STRING" "ACCOUNT KEY" :parse-fn read-string :default nil]
   ["-h" "--host STRING" "ACCOUNT HOST" :parse-fn read-string :default nil]
   ["-l" "--level LEVEL" "Consistency Level(eventual, session, staleness, strong, prefix"]
   :missing  (str "--level " (cli/one-of consistency-levels))
   :parse-fn read-string
   :validate [consistency-levels (cli/one-of consistency-levels)]])


(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (pprint opts)
  (let [host (:host opts)
        key  (:key opts)
        level-name (:level opts)
        level      ((consistency-levels level-name) opts)]
  (merge tests/noop-test
         opts
         {:name            (str "cosmos db consistency level=" level-name " ")
          :os              debian/os
          :db              (db/db opts)
          :client          (Client. nil host key level)
          :pure-generators true})
  )
  )

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (pprint args)
  (cli/run! (merge (cli/single-test-cmd {:test-fn cosmosdb-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))