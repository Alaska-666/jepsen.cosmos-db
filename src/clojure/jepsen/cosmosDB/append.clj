(ns jepsen.cosmosDB.append
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
                     [pprint :refer [pprint]]]
            [jepsen [client :as client]
                    [util :as util :refer [timeout]]
                    [independent :as independent]]
            [dom-top.core :refer [with-retry]]
            [slingshot.slingshot :refer [try+]]
            [jepsen.tests.cycle.append :as list-append]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosException TransactionalBatch TransactionalBatchOperationResult)
           (com.azure.cosmos.implementation RetryWithException ConflictException)
           (mipt.bit.utils MyList)
           (java.util HashMap)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        400)
(def partitionKeyPath  "/key")

(defn mop!
  "Applies a transactional micro-operation to a connection."
  [test container [f k v :as mop]]
  (info :mop mop)
  (case f
    :r      [f k (vec (c/read-item container k))]
    :append (let [res  (c/upsert-item container k v)]
              mop)
    )
  )

(defn update-batch!
  [container batch appends [f k v :as mop]]
  (info :update-batch mop)
  (case f
    :r       (c/update-batch-read container batch k)
    :append  (c/update-batch-append container batch appends k v)
    )
  )

(def operations
  {:READ   :r
   :UPSERT :append})


(defn processing-results!
  [container ^TransactionalBatchOperationResult result]
  (info :in-processing-results (.getOperationType (.getOperation result)))
  (let [operation (.toString (.getOperationType (.getOperation result)))
        f         (get operations operation)
        item      (.getItem result MyList)
        k         (.getLongId item)
        values    (.getValues item)
        v         (.getLastValue values)]
    (pprint (f k v))
  (case operation
    :READ    [f k (vec values)]
    :UPSERT  [f k v]
    (info :processing-results "jopa"))
  ))

(defrecord Client [conn account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/build-client node account-host account-key consistency-level)))

  (setup! [this test]
    (try
      (let [db (c/db conn databaseName)]
        (c/create-container! db containerName throughput partitionKeyPath))

      (catch CosmosException e#
        (condp re-find (.getMessage e#)
          #"Resource with specified id, name, or unique index already exists"
          (info "Container already exists.")
          (throw e#))
        )
      )
    )

  (invoke! [this test op]
    (try+
      ;(timeout 5000 (assoc op :type :info, :error :timeout)
      ;         (let [txn       (:value op)
      ;               db        (c/db conn databaseName)
      ;               container (c/container db containerName)
      ;               txn'      (mapv (partial mop! test container) txn)]
      ;           (assoc op :type :ok, :value txn')))
      (info :count (count (:value op)))

      (timeout 10000 (assoc op :type :info, :error :timeout)
               (let [txn' (if (<= (count (:value op)) 1)
                            (let [db        (c/db conn databaseName)
                                  container (c/container db containerName)]
                              [(mop! test container (first (:value op)))])

                            ; We need a transaction
                            (let [db        (c/db conn databaseName)
                                  container (c/container db containerName)
                                  batch     (c/create-transactional-batch nil)
                                  appends   (HashMap.)]
                              (pprint (:value op))
                              (mapv (partial update-batch! container batch appends) (:value op))
                              (let [response (c/execute-batch container batch)]
                                (pprint (.getResults response))
                                (if (not (.isSuccessStatusCode response))
                                  (assoc op :type :fail, :value :transaction-fail)
                                  (mapv (partial processing-results! container) (.getResults response)))
                                ))
                            )]
                 (assoc op :type :ok, :value txn'))
               )

      (catch RetryWithException e
        (assoc op :type :fail, :error :conflicting-request))

      (catch ConflictException e
        (assoc op :type :fail, :error :creation-conflict))

      (catch CosmosException e
        (assoc op :type :fail, :error :cosmos-exception))
      )
    )

  (teardown! [this test])

  (close! [_ test]
    (try
      (.close conn)
      (catch CosmosException e nil)
      )))

(def consistency-models
  {:eventual  [:read-your-writes]
   :session   [:causal]
   :staleness [:sequential]
   :strong    [:linearizable]
   :prefix    [:PRAM]})

(defn workload
  "A generator, client, and checker for a list-append test."
  [opts]
  (let [host               (:host opts)
        key                (:key opts)
        consistency-level  (get c/consistency-levels (:consistency opts))]
    (assoc (list-append/test {:key-count          10
                              :key-dist           :exponential
                              :max-txn-length     (:max-txn-length opts 4)
                              :max-writes-per-key (:max-writes-per-key opts)})
      :client (Client. nil host key consistency-level))))