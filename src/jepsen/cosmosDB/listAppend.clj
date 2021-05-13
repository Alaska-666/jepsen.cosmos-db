(ns jepsen.cosmosDB.listAppend
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
             [pprint :refer [pprint]]]
            [jepsen [client :as client]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(def databaseName   "AzureJepsenTestDB")
(def containerName  "JepsenTestContainer")

(defrecord Client [conn database container account-host account-key consistency-level]
  client/Client
  (open! [this test node]
      (assoc this
        :conn       (c/build-client node account-host account-key consistency-level)
        :database   (c/createDatabaseIfNotExists conn databaseName)
        :container  nil)
    )

  (setup! [this test])

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test]))