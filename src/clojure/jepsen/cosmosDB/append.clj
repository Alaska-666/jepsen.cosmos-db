(ns jepsen.cosmosDB.append
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [clojure [string :as str]
                     [pprint :refer [pprint]]]
            [jepsen [client :as client]
                    [util :as util :refer [timeout]]
                    [independent :as independent]]
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
  (pprint mop)
  (case f
    :r      [f k (vec (:value (c/read-item container k)))]
    :append (let [res  (c/upsert-item container k {:value v})]
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

  ;(invoke! [this test op]
  ;  (pprint test)
  ;  (c/with-errors op
  ;                 (timeout 5000 (assoc op :type :info, :error :timeout)
  ;                          (let [txn       (:value op)
  ;                                txn'      (mapv (partial mop! test container) txn)]
  ;                            (assoc op :type :ok, :value txn')))))

  (invoke! [_ test op]
    (pprint test)
    (let [[k v] (:value op)]
      (try+
        (case (:f op)
          :r (let [value (c/read-item container k)]
                  (assoc op :type :ok, :value (independent/tuple k value)))

          :append (do c/upsert-item container k {:value v}
                      (assoc op :type :ok))
          (pprint "aaaaa")
          )


        (catch SocketTimeoutException e
          (assoc op
            :type  (if (= :read (:f op)) :fail :info)
            :error :timeout))
        )
      )
    )

  (teardown! [this test])

  (close! [_ test]
    (try
      (if (not (nil? container)) (.delete container))
      (if (not (nil? database))  (.delete database))
      (.close conn)
      (catch CosmosException e nil)
      )))

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