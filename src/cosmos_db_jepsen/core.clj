(ns cosmos-db-jepsen.core
  (:require [jepsen.cli :as cli]
            [jepsen.tests :as tests]))

;(defn -main
;  "Handles command line arguments. Can either run a test, or a web server for
;  browsing results."
;  [& args]
;  (cli/run! (merge (cli/single-test-cmd {:test-fn  mongodb-test
;                                         :opt-spec cli-opts})
;                   (cli/test-all-cmd {:tests-fn (partial all-tests mongodb-test)
;                                      :opt-spec cli-opts})
;                   (cli/serve-cmd))
;            args))


(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         {:pure-generators true}
         opts))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn cosmosdb-test})
                   (cli/serve-cmd))
            args))