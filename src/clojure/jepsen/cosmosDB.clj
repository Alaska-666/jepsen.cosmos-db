(ns jepsen.cosmosDB
  (:require [clojure.tools.logging :refer [info warn]]
            [clojure [string :as str]
                     [pprint :refer [pprint]]]
            [jepsen [cli :as cli]
                    [checker :as checker]
                    [tests :as tests]
                    [generator :as gen]
                    [util :as util :refer [parse-long]]]
            [jepsen.os.debian :as debian]
            [jepsen.cosmosDB [db :as db]
                             [append :as append]
                             [nemesis :as nemesis]]))

(def workloads
  {:list-append append/workload
   :none        (fn [_] tests/noop-test)})

(def special-nemeses
  "A map of special nemesis names to collections of faults"
  {:none []
   :all  [:pause :kill :partition :clock :member]})

(defn parse-nemesis-spec
  "Takes a comma-separated nemesis string and returns a collection of keyword
  faults."
  [spec]
  (->> (str/split spec #",")
       (map keyword)
       (mapcat #(get special-nemeses % [%]))))

(def cli-opts
  "Additional command line options."
  [["-c" "--consistency LEVEL" "What level of consistency we should set: eventual, session, staleness, strong, prefix."
    :default :strong
    :parse-fn keyword
    :validate [#{:eventual
                 :session
                 :staleness
                 :strong
                 :prefix}
               "Should be one of eventual, session, staleness, strong, prefix."]]

   [nil "--key STRING" "Azure Cosmos DB account key."
    :default "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="]

   [nil "--host STRING" "Azure Cosmos DB account host."
    :default "https://localhost:8081"]

   [nil "--singleton-txns" "If set, execute even single operations in a transactional context."
    :default false]

   [nil "--nemesis FAULTS" "A comma-separated list of nemesis faults to enable"
    :parse-fn parse-nemesis-spec
    :validate [(partial every? #{:pause :kill :partition :clock :member})
               "Faults must be pause, kill, partition, clock, or member, or the special faults all or none."]]

   ["-w" "--workload NAME" "What workload should we run?"
    :parse-fn keyword
    :validate [workloads (cli/one-of workloads)]]

   ["-r" "--rate HZ" "Approximate number of requests per second, total"
    :default 1000
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]

   [nil "--max-txn-length NUM" "Maximum number of operations in a transaction."
    :default  4
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer"]]

   [nil "--max-writes-per-key NUM" "Maximum number of writes to any given key."
    :default  256
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer."]]
   ])

(defn cosmosdb-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (let [workload-name (:workload opts)
        workload      ((workloads workload-name) opts)
        db            (db/db opts)
        nemesis       (nemesis/nemesis-package
                        {:db        db
                         :nodes     (:nodes opts)
                         :faults    (:nemesis opts)
                         :partition {:targets [:primaries]}
                         :pause     {:targets [nil :one :primaries :majority :all]}
                         :kill      {:targets [nil :one :primaries :majority :all]}
                         :interval  (:nemesis-interval opts)})]
    (merge tests/noop-test
           opts
           {:name "cosmos db"
            :pure-generators true
            :os   debian/os
            :db   db
            :checker (checker/compose
                       {:perf       (checker/perf
                                      {:nemeses (:perf nemesis)})
                        :clock      (checker/clock-plot)
                        :stats      (checker/stats)
                        :exceptions (checker/unhandled-exceptions)
                        :workload   (:checker workload)})
            :client    (:client workload)
            ;:nemesis   (:nemesis nemesis)
            :generator (gen/phases
                         (->> (:generator workload)
                              (gen/stagger (/ (:rate opts)))
                              (gen/nemesis (:generator nemesis))
                              (gen/time-limit (:time-limit opts))))})))


(def all-workloads
  "A collection of workloads we run by default."
  (remove #{:none} (keys workloads)))

(def workloads-expected-to-pass
  "A collection of workload names which we expect should actually pass."
  (remove #{} all-workloads))

(def all-nemeses
  "Combinations of nemeses for tests"
  [[]
   [:pause :kill :partition :clock :member]])

(defn all-test-options
  "Takes base cli options, a collection of nemeses, workloads, and a test count,
  and constructs a sequence of test options."
  [cli nemeses workloads]
  (for [n nemeses, w workloads, i (range (:test-count cli))]
    (assoc cli
      :nemesis   n
      :workload  w)))


(defn all-tests
  "Turns CLI options into a sequence of tests."
  [test-fn cli]
  (let [nemeses   (if-let [n (:nemesis cli)] [n]  all-nemeses)
        workloads (if-let [w (:workload cli)] [w]
                                              (if (:only-workloads-expected-to-pass cli)
                                                workloads-expected-to-pass
                                                all-workloads))]
    (->> (all-test-options cli nemeses workloads)
         (map test-fn))))


(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  cosmosdb-test
                                         :opt-spec cli-opts})
                   (cli/test-all-cmd {:tests-fn (partial all-tests cosmosdb-test)
                                      :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))