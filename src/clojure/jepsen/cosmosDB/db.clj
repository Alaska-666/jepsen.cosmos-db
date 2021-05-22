(ns jepsen.cosmosDB.db
  (:require [clojure.tools.logging :refer :all]
            [jepsen [db :as db]]
            [jepsen.cosmosDB [client :as c]])
  (:import (com.azure.cosmos.implementation RetryWithException)))

(def dir     "/opt/cosmosDB")
(def logfile (str dir "/cosmosDB.log"))
(def databaseName      "AzureJepsenTestDB")

(defn db
  "???"
  [opts]
  (reify db/DB
    (setup! [_ test node]
      (info node "setup! cosmos db")
      (let [host               (:host opts)
            key                (:key opts)
            consistency-level  (get c/consistency-levels (:consistency opts))
            client (c/build-client node host key consistency-level)]
        (try c/create-database! client databaseName
             (catch RetryWithException e#
               (condp re-find (.getMessage e#)
                 #"Resource with specified id, name, or unique index already exists"
                 nil

                 (throw e#)))
             )
        (.close client)))

    (teardown! [_ test node]
      (info node "tearing down cosmos db"))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))