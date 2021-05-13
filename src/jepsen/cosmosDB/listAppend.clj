(ns jepsen.cosmosDB.listAppend
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
                     [pprint :refer [pprint]]]
            [jepsen [client :as client]
                    [util :as util :refer [timeout]]]
            [jepsen.tests.cycle.append :as list-append]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosException)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        400)
(def partitionKeyPath  "/id")

(defrecord Client [conn database container account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (let [conn      (c/build-client node account-host account-key consistency-level)
          database  (c/createDatabaseIfNotExists conn databaseName)]
      (assoc this
        :conn       conn
        :database   database
        :container  (c/createContainerIfNotExists database containerName throughput partitionKeyPath))
      )
    )

  (setup! [this test])

  (invoke! [this test op]
    (let [txn (:value op)]
      (c/with-errors op
         (timeout 5000 (assoc op :type :info, :error :timeout)
            (let [txn' (if (and (<= (count txn) 1) (not (:singleton-txns test)))
               ; We can run without a transaction
               (let [db (c/db conn db-name
                              {:read-concern  (:read-concern test)
                               :write-concern (:write-concern test)})]
                 [(apply-mop! test db nil (first txn))])

               ; We need a transaction
               (let [db (c/db conn db-name test)]
                 (with-open [session (c/start-session conn)]
                   (let [opts (txn-options test (:value op))
                         body (c/txn
                                ;(info :txn-begins)
                                (mapv (partial apply-mop!
                                               test db session)
                                      (:value op)))]
                     (.withTransaction session body opts)))))]
              (assoc op :type :ok, :value txn'))))))

  (teardown! [this test])

  (close! [_ test]
    (try
      (if (not (nil? container)) (.delete container))
      (if (not (nil? database))  (.delete database))
      (catch CosmosException e nil)
      ))
  )


(defn workload
  "A generator, client, and checker for a list-append test."
  [opts]
  (assoc (list-append/test {:key-count          10
                            :key-dist           :exponential
                            :max-txn-length     (:max-txn-length opts 4)
                            :max-writes-per-key (:max-writes-per-key opts)
                            :consistency-models [:strong-snapshot-isolation]})
    :client (Client. nil)))