(ns jepsen.cosmosDB.append
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
                     [pprint :refer [pprint]]]
            [jepsen [client :as client]
                    [util :as util :refer [timeout]]]
            [jepsen.tests.cycle.append :as list-append]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosException
                             ConsistencyLevel)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        400)
(def partitionKeyPath  "/id")

(defn apply-mop!
  "Applies a transactional micro-operation to a connection."
  [test container [f k v :as mop]]
  (case f
    :r      [f k (vec (:value (c/read-object container k)))]
    :append (let [res  (c/upsert-object container k {:value v})]
              (info :res res)
              mop))
  )

(defrecord Client [conn database container account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (let [conn      (c/build-client node account-host account-key consistency-level)
          database  (c/createDatabaseIfNotExists conn databaseName)]
      (assoc this
        :conn       conn
        :database   database
        :container  (c/createContainerIfNotExists database containerName throughput partitionKeyPath))
      )
    )

  (setup! [this test])

  (invoke! [this test op]
    (pprint test)
    (let [txn (:value op)]
      (c/with-errors op
         (timeout 5000 (assoc op :type :info, :error :timeout)
            (let [txn' (if (and (<= (count txn) 1) (not (:singleton-txns test)))
               ; We can run without a transaction
               [(apply-mop! test container (first txn))]

               ; We need a transaction
               ;(let [db (c/db conn db-name test)]
               ;  (with-open [session (c/start-session conn)]
               ;    (let [opts (txn-options test (:value op))
               ;          body (c/txn
               ;                 ;(info :txn-begins)
               ;                 (mapv (partial apply-mop!
               ;                                test db session)
               ;                       (:value op)))]
               ;      (.withTransaction session body opts))))
               )]
              (assoc op :type :ok, :value txn'))))))

  (teardown! [this test])

  (close! [_ test]
    (try
      (if (not (nil? container)) (.delete container))
      (if (not (nil? database))  (.delete database))
      (.close conn)
      (catch CosmosException e nil)
      ))
  )

(def consistency-levels
  {:eventual  (ConsistencyLevel/EVENTUAL)
   :session   (ConsistencyLevel/SESSION)
   :staleness (ConsistencyLevel/BOUNDED_STALENESS)
   :strong    (ConsistencyLevel/STRONG)
   :prefix    (ConsistencyLevel/CONSISTENT_PREFIX)})

(defn workload
  "A generator, client, and checker for a list-append test.
  FIXME: fix consistency-models and other options !!!!!!
  "
  [opts]
  (pprint opts)
  (let [host               (:host opts)
        key                (:key opts)
        consistency-level  (get consistency-levels (:consistency opts))]
    (assoc (list-append/test {:key-count          10
                              :key-dist           :exponential
                              :max-txn-length     (:max-txn-length opts 4)
                              :max-writes-per-key (:max-writes-per-key opts)
                              :consistency-models [:strong-snapshot-isolation]})
      :client (Client. nil nil nil host key consistency-level))))