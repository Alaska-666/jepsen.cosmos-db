(ns jepsen.cosmosDB.db
  (:require [clojure.tools.logging :refer :all]
            [jepsen [db :as db]]))

(def dir     "/opt/cosmosDB")
(def logfile (str dir "/cosmosDB.log"))

(defn db
  "???"
  (reify db/DB
    (setup! [_ test node]
      (info node "setup! cosmos db"))

    (teardown! [_ test node]
      (info node "tearing down cosmos db"))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))