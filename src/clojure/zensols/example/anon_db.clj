(ns zensols.example.anon-db
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:require [zensols.actioncli.dynamic :as dyn]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.resource :as res]
            [zensols.nlparse.parse :as p]
            [zensols.dataset.db :as db :refer (with-connection)]))

(def ^:private corpora
  [{:class-label "answer"
    :res ["answers-input-1.txt"
          "answers-input-2.txt"]}
   {:class-label "expressive"
    :res ["expressives-input-1.txt"
          "expressives-input-2.txt"]}
   {:class-label "question"
    :res ["questions-input-1.txt"
          "questions-input-2.txt"]}])

(def ^:private corpora-dir "corpora")

(defonce ^:private conn-inst (atom nil))

(defn reset-instances []
  (reset! conn-inst nil))

(dyn/register-purge-fn reset-instances)

(defn- parse-utterances [add-fn]
  (let [cnt (atom 0)]
    (doseq [corpus corpora]
      (doseq [res-name (:res corpus)]
        (let [res-name (format "%s/%s" corpora-dir res-name)
              res (io/resource res-name)]
          (log/infof "parsing corpus: %s" res-name)
          (with-open [reader (->> res io/reader)]
            (->> reader
                 (line-seq)
                 (map p/parse)
                 (map #(add-fn (-> (swap! cnt inc) str) % (:class-label corpus)))
                 doall)))))))

(defn- connection []
  (swap! conn-inst #(or % (db/elasticsearch-connection
                           "example"
                           :create-instances-fn parse-utterances))))

(defn load-corpora []
  (with-connection (connection)
    (db/instances-load)))

(defn anons [& opts]
  (with-connection (connection)
    (apply db/instances opts)))

(defn anon-by-id [& opts]
  (with-connection (connection)
    (apply db/instance-by-id opts)))

(defn divide-by-set [train-ratio & opts]
  (with-connection (connection)
    (apply db/divide-by-set train-ratio opts)))

(defn turn-on-test-set [test?]
  (with-connection (connection)
    (db/set-default-set-type (if test? :test :train))))

(defn- write-dataset-instances
  "Write the dataset by train/test bucket to the file system."
  []
  (doseq [set-type [:train :test]]
    (let [out-file (->> (format "%s.txt" (name set-type))
                        (res/resource-path :analysis-report))]
      (with-open [writer (io/writer out-file)]
        (binding [*out* writer]
          (->> (anons :set-type set-type)
               (map #(->> % :instance :text))
               sort
               (map println)
               doall))))))

(defn- main [& actions]
  (->> actions
       (map (fn [action]
              (case action
                -2 (dyn/purge)
                -1 (reset-instances)
                0 (load-corpora)
                1 (with-connection (connection)
                    (db/instance-count))
                2 (with-connection (connection)
                    (db/stats))
                3 (clojure.pprint/pprint (connection))
                4 (write-dataset-instances))))
       doall))

(def load-corpora-command
  "CLI command to load the corpora into elastic search"
  {:description "load corpus data -> elastic search"
   :options
   [(lu/log-level-set-option)]
   :app (fn [& _]
          (load-corpora))})
