(ns jepsen.cosmosDB.listAppend
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [jepsen [client :as client]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(defrecord Client [conn account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/build-client node account-host account-key consistency-level)))

  (setup! [this test])

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test]))