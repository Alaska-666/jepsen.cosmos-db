(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:require [clojure.tools.logging :refer :all]
            [jepsen [client :as client]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(def account-key "6jL5JZKYLGGDkSUVb2xkgNbieOkSwhgbOzizB4COjoKummXYf174iWQo8iTg1FEolNPMNiYl70kqDVQtUU6eug==")
(def account-host "https://4d76afcd-0ee0-4-231-b9ee.documents.azure.com:443/")


(defn ^CosmosClient build-client
  "???"
  [node ^String host ^String acc-key ^ConsistencyLevel level]
  (let [builder (CosmosClientBuilder.)]
    (-> builder
        (.endpoint host)
        (.key acc-key)
        (.consistencyLevel level)
        (.buildClient))
  ))

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (build-client node account-host account-key ConsistencyLevel/EVENTUAL)))

  (setup! [this test])

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test]))