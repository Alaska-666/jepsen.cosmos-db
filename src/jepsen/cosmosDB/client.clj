(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(defn ^CosmosClient build-client
  "???"
  [node ^String host ^String key ^ConsistencyLevel level]
  (((CosmosClientBuilder .endpoint host) .key key) .consistencyLevel level) .buildClient)

