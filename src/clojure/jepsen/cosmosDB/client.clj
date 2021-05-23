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
                             CosmosException
                             CosmosContainer
                             CosmosAsyncClient TransactionalBatch TransactionalBatchResponse)
           (com.azure.cosmos.models CosmosContainerProperties
                                    CosmosItemRequestOptions
                                    PartitionKey
                                    ThroughputProperties)
           (java.util Collections Arrays ArrayList)
           (mipt.bit.utils MyList)
           (clojure.lang ExceptionInfo)
           (com.azure.cosmos.implementation NotFoundException RetryWithException ConflictException)))

(def partitionKey "key")

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

(def consistency-levels
  {:eventual  (ConsistencyLevel/EVENTUAL)
   :session   (ConsistencyLevel/SESSION)
   :staleness (ConsistencyLevel/BOUNDED_STALENESS)
   :strong    (ConsistencyLevel/STRONG)
   :prefix    (ConsistencyLevel/CONSISTENT_PREFIX)})

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

(defn ^CosmosDatabase create-database!
  [^CosmosClient client ^String databaseName]
  ;CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
  ;database = client.getDatabase(databaseResponse.getProperties().getId());
  (let [id (.getId (.getProperties (.createDatabaseIfNotExists client databaseName)))]
    (.getDatabase client id)
    )
  )

(defn ^CosmosDatabase db
  [^CosmosClient client ^String databaseName]
  (try
    (.getDatabase client databaseName)

    (catch CosmosException e
      nil))
  )

(defn ^CosmosContainer create-container!
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

(defn ^CosmosContainer container
  [^CosmosDatabase database ^String name]
  (try
    (.getContainer database name)

    (catch CosmosException e
      nil)
    )
  )

(defmacro with-errors
  "Takes an operation and a body; evals body, turning known errors into :fail
  or :info ops."
  [op & body]
  `(try ~@body
        (catch ExceptionInfo e#
          (warn e# "Caught ex-info")
          (assoc ~op :type :info, :error [:ex-info (.getMessage e#)]))

        (catch RetryWithException e#
          (warn e#)
          (assoc ~op :type :fail, :error :conflicting-request))

        (catch ConflictException e#
          (warn e#)
          (assoc ~op :type :fail, :error :creation-conflict))
        )
  )

(defn ^MyList get-item
  [^CosmosContainer container id]
  (let [id (.toString id)]
    (.getItem (.readItem container id (PartitionKey. partitionKey) MyList)))
  )

(defn create-empty-item
  [^CosmosContainer container id]
  ;CosmosItemRequestOptions cosmosItemRequestOptions = new CosmosItemRequestOptions();
  ;CosmosItemResponse<MyList> item = container.createItem(new MyList(id, Collections.emptyList()), PartitionKey, cosmosItemRequestOptions);
  (try
    (let [cosmosItemRequestOptions (CosmosItemRequestOptions.)
          id (.toString id)
          item (.createItem container (MyList. id partitionKey (. Collections emptyList))  (PartitionKey. partitionKey) cosmosItemRequestOptions)]
      (info
        :create   true
        :item     (.getItem item)
        :duration (.getDuration item)
        ))
    (catch ConflictException e nil)
    )
  )

(defn read-item
  "Find a object by ID"
  [^CosmosContainer container id]
  ;Object object = container.readItem(id, PartitionKey, Object.class).getItem();
  (try
    (let [^MyList item (get-item container id)]
      (info :read item)
      (.getValues item))
    (catch NotFoundException e#
      (create-empty-item container id)
      (let [^MyList item (get-item container id)]
        (info :read item)
        (.getValues item)
        )
      )
    )
  )


(defn create-item
  [^CosmosContainer container id values]
  ;CosmosItemRequestOptions cosmosItemRequestOptions = new CosmosItemRequestOptions();
  ;CosmosItemResponse<MyList> item = container.createItem(new MyList(id, Arrays.asList(values)), PartitionKey, cosmosItemRequestOptions);
  (let [cosmosItemRequestOptions (CosmosItemRequestOptions.)
        id (.toString id)
        item (.createItem container (MyList. id partitionKey (.asList Arrays values)) (PartitionKey. partitionKey) cosmosItemRequestOptions)]
    (info
      :item     (.getItem item)
      :duration (.getDuration item))
    )
  )

(defn ^TransactionalBatch create-transactional-batch
  [opts]
  ;TransactionalBatch batch = TransactionalBatch.createTransactionalBatch(partitionKey);
  (TransactionalBatch/createTransactionalBatch (PartitionKey. partitionKey))
  )

(defn update-batch-read
  [^CosmosContainer container ^TransactionalBatch batch id]
  (try
    (.readItem container (.toString id) (PartitionKey. partitionKey) MyList)
    (catch NotFoundException e
      (create-empty-item container (.toString id))))

  (.readItemOperation batch (.toString id))
  )

(defn ^MyList get-item-or-create-if-not-exists
  [^CosmosContainer container id]
  (try
    (let [item (.readItem container (.toString id) partitionKey MyList)]
      (.getItem item))
    (catch NotFoundException e
      (create-empty-item container id)
      (get-item container id)))
  )

(defn add-value
  [appends id newValue]
  (let [values (.get appends id)]
    (.add values newValue)
    (.put appends id values)
    )
  )

(defn update-batch-append
  [^CosmosContainer container ^TransactionalBatch batch appends key newValue]
  (info :appends-before appends)
  (if (.containsKey appends key)
    (add-value appends key newValue)
    (.put appends key (ArrayList. (Collections/singletonList newValue))))
  (info :appends-after appends)

  (let [oldMyList (get-item-or-create-if-not-exists container key)]
    (.upsertItemOperation batch (MyList. (.toString key) partitionKey (concat (.getValues oldMyList) (key appends))))
    )
  )

(defn ^TransactionalBatchResponse execute-batch
  [^CosmosContainer container ^TransactionalBatch batch]
  ;TransactionalBatchResponse response = container.executeTransactionalBatch(batch);
  (.executeTransactionalBatch container batch)
  )

(defn upsert-item
  [^CosmosContainer container id newValue]
  ;MyList list = container.readItem(id, PartitionKey, MyList.class).getItem();
  ;list.getValues().add(newValue);
  ;CosmosItemResponse<MyList> item = container.upsertItem(list);
  (try
    (let [^MyList item (get-item container id)]
      (info :oldItem item)
      (.add (.getValues item) newValue)
      (info :newItem item)
      (.upsertItem container item)
      (.getValues item))
    (catch NotFoundException e#
      (info :exception (.getMessage e#))
      (create-empty-item container id)
      (let [^MyList item (get-item container id)]
        (info :oldItem item)
        (.add (.getValues item) newValue)
        (info :newItem item)
        (.upsertItem container item)
        (.getValues item))
      )
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