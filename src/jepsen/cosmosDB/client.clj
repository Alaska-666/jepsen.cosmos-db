(ns jepsen.cosmosDB.client
  "Wraps the Cosmos DB Java client."
  (:require [clojure.walk :as walk]
            [clojure.tools.logging :refer [info warn]]
            [jepsen [util :as util :refer [timeout]]]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import (com.azure.cosmos CosmosClientBuilder
                             CosmosClient
                             ConsistencyLevel)))

(defn ^CosmosClient build-client
  "???"
  [node ^String host ^String key ^ConsistencyLevel level]
  (((CosmosClientBuilder .endpoint host) .key key) .consistencyLevel level) .buildClient)

