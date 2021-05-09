(ns jepsen.cosmosDB.db
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
             [control :as c]
             [db :as db]
             [tests :as tests]]
            [jepsen.control.util :as cu]))


(defn db
  "???"
  [opts]
  (reify db/DB
    (setup! [_ test node]
      (info node "setup cosmos db"))

    (teardown! [_ test node]
      (info node "tearing down cosmos db"))))