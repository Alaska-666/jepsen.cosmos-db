(ns jepsen.cosmosDB.db
  (:require [clojure.tools.logging :refer :all]
            [jepsen [db :as db]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos CosmosException)))

(def dir     "/opt/cosmosDB")
(def logfile (str dir "/cosmosDB.log"))
(def databaseName      "AzureJepsenTestDB")
(def containerName     "JepsenTestContainer")

(defn db
  "Create and delete database"
  [opts]
  (reify db/DB
    (setup! [_ test node]
      (info node "setup! cosmos db")
      (let [host               (:host opts)
            key                (:key opts)
            consistency-level  (get c/consistency-levels (:consistency opts))
            client             (c/build-client node host key consistency-level)]
        (try
          (c/create-database! client databaseName)
          (catch CosmosException e#
            (condp re-find (.getMessage e#)
              #"Resource with specified id, name, or unique index already exists"
              (info node "Database already exists.")
              (throw e#))))
        (.close client)))

    (teardown! [_ test node]
      (info node "tearing down cosmos db")
      (try
        (let [host               (:host opts)
              key                (:key opts)
              consistency-level  (get c/consistency-levels (:consistency opts))
              client             (c/build-client node host key consistency-level)
              db                 (c/db client databaseName)
              container          (c/container db containerName)]
          (if (not (nil? container)) (.delete container))
          (if (not (nil? db))  (.delete db))
          (.close client))
        (catch CosmosException e nil))
      )

    db/LogFiles
    (log-files [_ test node]
      [logfile])))