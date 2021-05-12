(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [jepsen [client :as client]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))


(defn ^CosmosClient build-client
  "???"
  [node ^String host ^String acc-key ^ConsistencyLevel level]
  (let [builder (CosmosClientBuilder.)]
    (-> builder
        (.endpoint host)
        (.key acc-key)
        (.consistencyLevel level)
        (.contentResponseOnWriteEnabled true)
        (.buildClient))
  ))

(defn ^CosmosClient build-async-client
  "???"
  [node ^String host ^String acc-key ^ConsistencyLevel level]
  (let [builder (CosmosClientBuilder.)]
    (-> builder
        (.endpoint host)
        (.key acc-key)
        (.consistencyLevel level)
        (.contentResponseOnWriteEnabled true)
        (.buildAsyncClient))
    ))