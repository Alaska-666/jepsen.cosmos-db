(ns jepsen.cosmosDB.listAppend
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
             [pprint :refer [pprint]]]
            [dom-top.core :refer [with-retry]]
            [jepsen [client :as client]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        400)
(def partitionKeyPath  "/id")

(defrecord Client [conn database container account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/build-client node account-host account-key consistency-level))
    )

  (setup! [this test]
    (let [database  (c/createDatabaseIfNotExists conn databaseName)]
      (assoc this
        :database   database
        :container  (c/createContainerIfNotExists database containerName throughput partitionKeyPath))
      )
    )

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test])
    (.delete container)
    (.delete database)
  )