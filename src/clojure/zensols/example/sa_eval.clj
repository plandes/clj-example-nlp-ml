(ns zensols.example.sa-eval
  (:require [clojure.tools.logging :as log]
            [clojure.set :refer (union)])
  (:require [zensols.actioncli.dynamic :refer (dyn-init-var) :as dyn]
            [zensols.model.classifier :as cl]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec])
  (:require [zensols.example.sa-feature :as sf]
            [zensols.example.anon-db :as adb]))

;; why does this return dups?
;; http://codereview.stackexchange.com/questions/12979/powerset-in-clojure
(defn powerset [ls]
  (if (empty? ls) '(())
      (union (powerset (next ls))
             (map #(conj % (first ls)) (powerset (next ls))))))

(defn feature-sets-set []
  (let [candidates '(token-count
                     pos-tag-ratio-noun
                     pos-tag-ratio-wh
                     pos-tag-ratio-noun
                     pos-tag-ratio-adjective
                     pos-last-tag
                     pos-first-tag
                     stopword-count
                     dep-tree-id)]
    {:set-1 '((token-count))
     :set-2 '((token-count
               pos-tag-ratio-verb
               pos-tag-ratio-adverb
               pos-tag-ratio-noun
               pos-tag-ratio-adjective))
     :set-power (->> candidates
                     powerset
                     (filter #(> (count %) 1)))
     :set-big '((token-count
                 pos-tag-ratio-noun
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag)
                (token-count
                 pos-tag-ratio-noun
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag
                 dep-tree-id)
                (pos-tag-ratio-noun
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag
                 dep-tree-id)
                (pos-tag-ratio-noun
                 pos-tag-ratio-verb
                 pos-tag-ratio-adjective
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag
                 dep-tree-id)
                (token-count
                 srl-argument-counts
                 pos-tag-ratio-noun
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag)
                (token-count
                 pos-tag-ratio-noun
                 pos-tag-ratio-wh
                 pos-last-tag
                 pos-first-tag
                 stopword-count
                 dep-tree-id))
     :set-best '((token-count
                  pos-tag-ratio-noun
                  pos-tag-ratio-wh
                  pos-first-tag
                  pos-last-tag
                  stopword-count
                  ))}))

(defonce ^:private cross-fold-instances-inst (atom nil))
(defonce ^:private train-test-instances-inst (atom nil))

(->> @cross-fold-instances-inst count)

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
  (binding [cl/*rand-fn* (fn [] (java.util.Random. 1))
            ec/*cross-fold-count* 2]
   (with-model-conf (create-model-config)
     (let [classifiers [;:fast
                        ;:lazy :tree
                        ;:meta :slow
                        ;:really-slow
                        :j48
                        ;:zeror
                        ]
           classifiers (->> zensols.model.weka/*classifiers*
                            keys)
           meta-set :set-best]
       (->> (map (fn [action]
                   (case action
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
                     8 (ec/compile-results classifiers meta-set
                                           :test-type :train-test)
                     9 (->> (ec/train-test-series
                             [:j48] :set-best {:start 0.1 :stop 1 :step 0.03})
                            ec/write-csv-train-test-series)
                     10 (ec/print-best-results [:j48] :set-best)))
                 actions)
            doall)))))
