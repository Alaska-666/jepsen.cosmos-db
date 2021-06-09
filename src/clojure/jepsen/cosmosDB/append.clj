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
           (com.azure.cosmos.implementation RetryWithException ConflictException RequestRateTooLargeException)
           (mipt.bit.utils Operation TransactionsExecute)
           (java.util ArrayList List)
           (org.json JSONObject)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        1000)
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

(defn update-operations!
  [^ArrayList operations [f k v :as mop]]
  (info :update-operations mop)
  (case f
    :r       (.add operations (Operation. "r" k v))
    :append  (.add operations (Operation. "append" k v))
    )
  )

(def operations
  {"r"   :r
   "append" :append})


(defn processing-results!
  [container ^JSONObject result]
  (info :in-processing-results (.toString result))
  (let [operation (.get result "type")
        f         (get operations operation)
        k         (Long/valueOf (.get result "key"))
        values    (if (.has result  "readResult") (.get result "readResult") [])
        v         (.get result "value")]
  (case operation
    "r"       [f k (vec values)]
    "append"  [f k v]))
  )

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
                                  operations   (ArrayList.)]
                              (mapv (partial update-operations! operations) (:value op))
                              (let [^List results (.execute (TransactionsExecute. container) operations)]
                                (if (not= (.size results) (count (:value op)))
                                  (assoc op :type :fail, :value :transaction-fail)
                                  (mapv (partial processing-results! container) results))
                                ))
                            )]
                 (assoc op :type :ok, :value txn'))
               )
      (catch RequestRateTooLargeException e
        (assoc op :type :fail, :error :request-rate-too-large))

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