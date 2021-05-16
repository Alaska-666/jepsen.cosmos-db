(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
             [pprint :refer [pprint]]]
            [jepsen [client :as client]]
            [jepsen.cosmosDB [MyList :as MyList]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel
                             CosmosDatabase
                             CosmosException
                             CosmosContainer
                             CosmosAsyncClient)
           (com.azure.cosmos.models CosmosContainerProperties
                                    CosmosItemRequestOptions
                                    PartitionKey
                                    ThroughputProperties)
           (jepsen.cosmosDB MyList)
           (java.util Collections Arrays)))


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

(defn ^CosmosContainer createContainerIfNotExists
  [^CosmosDatabase database ^String containerName throughput ^String partitionKeyPath]
  ;CosmosContainerProperties containerProperties =
  ;new CosmosContainerProperties(containerName, "/lastName");
  ;
  ;ThroughputProperties throughputProperties = ThroughputProperties.createManualThroughput(400);
  ;CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties, throughputProperties);
  ;container = database.getContainer(containerResponse.getProperties().getId());
  (let [containerProperties (CosmosContainerProperties. containerName partitionKeyPath)
        throughputProperties (. ThroughputProperties createManualThroughput throughput)
        containerResponse (.createContainerIfNotExists database containerProperties throughputProperties)]
    (.getContainer database (.getId (.getProperties containerResponse)))
    )
  )

(defmacro with-errors
  "Takes an operation and a body; evals body, turning known errors into :fail
  or :info ops."
  [op & body]
  `(try ~@body
        (catch CosmosException e#
          (condp re-find (.getMessage e#)
            ; This... seems like a bug too
            (assoc ~op :type :fail, :error [:ex-info (.getMessage e#)])
            (throw e#)))
        )
  )


(defn ^MyList read-item
  "Find a object by ID"
  [^CosmosContainer container id]
  ;Object object = container.readItem(id, new PartitionKey(id), Object.class).getItem();
  (.getItem (.readItem container id (PartitionKey. id) (. MyList class)))
  )

(defn create-empty-item
  [^CosmosContainer container id]
  ;CosmosItemRequestOptions cosmosItemRequestOptions = new CosmosItemRequestOptions();
  ;CosmosItemResponse<MyList> item = container.createItem(new MyList(id, Collections.emptyList()), new PartitionKey(id), cosmosItemRequestOptions);
  (let [cosmosItemRequestOptions (CosmosItemRequestOptions.)
        item (.createItem container (MyList. id (. Collections emptyList)) (PartitionKey. id) cosmosItemRequestOptions)]
    (info :item     (.getItem item))
          :duration (.getDuration item)
    )
  )

(defn create-item
  [^CosmosContainer container id values]
  ;CosmosItemRequestOptions cosmosItemRequestOptions = new CosmosItemRequestOptions();
  ;CosmosItemResponse<MyList> item = container.createItem(new MyList(id, Arrays.asList(values)), new PartitionKey(id), cosmosItemRequestOptions);
  (let [cosmosItemRequestOptions (CosmosItemRequestOptions.)
        item (.createItem container (MyList. id (.asList (. Arrays ) values)) (PartitionKey. id) cosmosItemRequestOptions)]
    (info :item     (.getItem item))
          :duration (.getDuration item)
    )
  )

(defn upsert-item
  [^CosmosContainer container id newValue]
  ;MyList list = container.readItem(id, new PartitionKey(id), MyList.class).getItem();
  ;list.getValues().add(newValue);
  ;CosmosItemResponse<MyList> item = container.upsertItem(list);
  (let [^MyList item (.getItem (.readItem container id (PartitionKey. id) (. MyList class)))]
    (.add (.getValues item) newValue)
    (.upsertItem container item)
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
