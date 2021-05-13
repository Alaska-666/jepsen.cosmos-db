(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
             [pprint :refer [pprint]]]
            [jepsen [client :as client]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel
                             CosmosDatabase
                             CosmosAsyncDatabase
                             CosmosAsyncClient)))


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

(defn ^CosmosAsyncClient build-async-client
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

(defn ^CosmosDatabase createDatabaseIfNotExists
  [^CosmosClient client ^String databaseName]
  ;CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
  ;database = client.getDatabase(databaseResponse.getProperties().getId());
  (let [id (.getId (.getProperties (.createDatabaseIfNotExists client databaseName)))]
    (.getDatabase client id)
    )
  )

;(defn ^CosmosAsyncDatabase createAsyncDatabaseIfNotExists
;  [^CosmosAsyncClient client ^String databaseName]
;  ;Mono<CosmosDatabaseResponse> databaseIfNotExists = client.createDatabaseIfNotExists(databaseName);
;  ;databaseIfNotExists
;  ;.flatMap(databaseResponse -> {
;  ;                              database = client.getDatabase(databaseResponse.getProperties().getId());
;  ;                              logger.info("Checking database " + database.getId() + " completed!\n");
;  ;                              return Mono.empty();
;  ;                              })
;  ;.block();
;  (let [databaseIfNotExists (.createDatabaseIfNotExists client databaseName)]
;    (-> databaseIfNotExists
;        (.flatMap
;          )
;        (.block)
;        ))
;  )