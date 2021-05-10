(ns jepsen.cosmosDB.listAppend
  (:require [clojure.tools.logging :refer :all]
            [jepsen [client :as client]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos ConsistencyLevel)))

(def account-key "6jL5JZKYLGGDkSUVb2xkgNbieOkSwhgbOzizB4COjoKummXYf174iWQo8iTg1FEolNPMNiYl70kqDVQtUU6eug==")
(def account-host "https://4d76afcd-0ee0-4-231-b9ee.documents.azure.com:443/")

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/build-client node account-host account-key ConsistencyLevel/EVENTUAL)))

  (setup! [this test])

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test]))