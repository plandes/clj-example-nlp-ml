(ns ^{:doc "Just like [[zensols.example.sa_eval]] but this shows how to use a
two pass cross validation.  See the [two pass configurator macro](https://plandes.github.io/clj-ml-model/codox/zensols.model.eval-classifier.html#var-with-two-pass)
for more information."
      :author "Paul Landes"}
    zensols.example.sa-tp-eval
  (:require [clojure.tools.logging :as log]
            [clojure.set :refer (union)])
  (:require [zensols.actioncli.dynamic :refer (dyn-init-var) :as dyn]
            [zensols.model.classifier :as cl]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec :refer (with-two-pass)])
  (:require [zensols.example.sa-tp-feature :as sf :refer (with-feature-context)]
            [zensols.example.anon-db :as adb]))

(defonce ^:private cross-fold-instances-inst (atom nil))
(defonce ^:private train-test-instances-inst (atom nil))

(defn feature-sets-set []
  {:set-best '((token-count
                pos-tag-ratio-noun
                pos-tag-ratio-wh
                pos-first-tag
                pos-last-tag
                stopword-count
                word-count-expressive
                word-count-question
                word-count-answer))
   :set-test-two-pass '((word-count-expressive
                         word-count-question
                         word-count-answer))})

(defn reset-instances []
  (reset! cross-fold-instances-inst nil)
  (reset! train-test-instances-inst nil))

(dyn/register-purge-fn reset-instances)

(defn- create-model-config []
  (letfn [(divide-by-set [divide-ratio]
            (adb/divide-by-set divide-ratio :shuffle? false)
            (reset! train-test-instances-inst nil))]
    (merge (sf/create-model-config)
           {:cross-fold-instances-inst cross-fold-instances-inst
            :train-test-instances-inst train-test-instances-inst
            :feature-sets-set (feature-sets-set)
            :divide-by-set divide-by-set})))

(defn- main [& actions]
  (let [classifiers [:j48]
        ;; easy to control whether a two pass cross validation is used
        two-pass? true
        meta-set :set-test-two-pass]
    (binding [cl/*rand-fn* (fn [] (java.util.Random. 1))
              ec/*cross-fold-count* 2]
      (with-two-pass (create-model-config)
          (if two-pass?
            {:id-key sf/id-key
             :anon-by-id-fn #(->> % adb/anon-by-id :instance)
             :anons-fn adb/anons})
        (with-feature-context (sf/create-context :anons-fn adb/anons
                                                 :set-type :train-test)
          (->> (map (fn [action]
                      (case action
                        -3 (ec/executing-two-pass?)
                        -2 (println (apply str (repeat 60 \-)))
                        -1 (adb/load-corpora)
                        0 (dyn/purge)
                        1 (reset-instances)
                        2 (time (ec/write-arff))
                        3 (ec/terse-results classifiers meta-set :only-stats? true)
                        4 (ec/eval-and-write classifiers meta-set)
                        5 (ec/create-model classifiers meta-set)
                        6 (->> (ec/create-model classifiers meta-set)
                               ec/train-model
                               ec/write-model)
                        7 (adb/divide-by-set 0.05)
                        8 (ec/compile-results classifiers meta-set)
                        9 (->> (ec/train-test-series
                                [:j48] :set-best {:start 0.1 :stop 1 :step 0.03})
                               ec/write-csv-train-test-series)
                        10 (ec/print-best-results classifiers meta-set)))
                    actions)
               doall))))))

(main 0 3)
