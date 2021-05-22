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
  (:import (com.azure.cosmos CosmosException
                             ConsistencyLevel)
           (java.net SocketTimeoutException)))

(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")
(def throughput        400)
(def partitionKeyPath  "/id")

(defn mop!
  "Applies a transactional micro-operation to a connection."
  [test container [f k v :as mop]]
  (pprint "in mop!")
  (pprint (str " f=" f " k=" k " v=" v))
  (case f
    :r      [f k (vec (:value (c/read-item container k)))]
    :append (let [res  (c/upsert-item container k {:value v})]
              ;(info :res res)
              mop)
    )
  )

(defrecord Client [conn account-host account-key consistency-level]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/build-client node account-host account-key consistency-level)))

  (setup! [this test]
    (with-retry [tries 5]
      (let [db   (c/db conn databaseName)]
        (let [container (c/create-container! db containerName throughput partitionKeyPath)]
          (info "Container created")))

      (catch CosmosException e
        (if (pos? tries)
          (do (info (.getMessage e))
              (Thread/sleep 5000)
              (retry (dec tries)))
          (throw e)))))

  (invoke! [this test op]
    (c/with-errors op
       (timeout 5000 (assoc op :type :info, :error :timeout)
          (let [txn       (:value op)
                db        (c/db conn databaseName)
                container (c/container db containerName)
                txn'      (mapv (partial mop! test container) txn)]
            (assoc op :type :ok, :value txn')))))

  (teardown! [this test])

  (close! [_ test]
    (try
      (.close conn)
      (catch CosmosException e nil)
      )))

(defn workload
  "A generator, client, and checker for a list-append test.
  FIXME: fix consistency-models and other options !!!!!!
  "
  [opts]
  (let [host               (:host opts)
        key                (:key opts)
        consistency-level  (get c/consistency-levels (:consistency opts))]
    (assoc (list-append/test {:key-count          10
                              :key-dist           :exponential
                              :max-txn-length     (:max-txn-length opts 4)
                              :max-writes-per-key (:max-writes-per-key opts)
                              :consistency-models [:strong-snapshot-isolation]})
      :client (Client. nil nil nil host key consistency-level))))