(ns zensols.example.wc-eval
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.dynamic :refer (defa-)]
            [zensols.util.zip :as z :refer (doentries)]
            [zensols.model.classifier :as cl]
            [zensols.model.weka :as w]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec]))

(def ^:dynamic *doc-defs*
  [{:res "corpora/answers-input-1.txt" :doc-type "ans"}
   {:res "corpora/expressives-input-1.txt" :doc-type "exp"}])

(defn- new-classifier []
  ;(weka.classifiers.trees.J48.)
  (weka.classifiers.bayes.NaiveBayes.)
;(weka.classifiers.rules.ZeroR.)
;(weka.classifiers.functions.LibSVM.)
;(weka.classifiers.functions.MultilayerPerceptron.)
  )

(defa- classifier-inst)

(defn- read-utterances [res doc-type]
  (with-open [reader (io/reader (io/resource res))]
    (->> reader
         line-seq
         (map (fn [line]
                {:text line :doc-type doc-type}))
         doall)))

(defn- create-simple-resources
  ([]
   (create-simple-resources *doc-defs*))
  ([defs]
   (->> defs
        (map (fn [{:keys [res doc-type]}]
               (read-utterances res doc-type)))
        (apply concat))))

(defn- doc-lines [name reader]
  (->> reader
       line-seq
       (map s/trim)
       (filter #(> (count %) 0))
       (take 500)
       (map #(array-map :doc-type name :text %))
       doall))

(defn- doc-line [name reader]
  (->> reader slurp
       (array-map :doc-type name :text)
       list))

(defn- create-zip-resources []
  (with-open [istream (io/input-stream (io/resource "docs.zip"))]
    (->> (doentries [istream eis entry]
           (when-let [name (->> (if-not (.isDirectory entry) (.getName entry))
                                (#(and % (re-find #".+\/([^.]+)" %)))
                                second)]
             (with-open [reader (io/reader eis)]
               {:result (doc-lines name reader)              ;(doc-line name reader)
                })))
         (remove nil?)
         (apply concat))))

(def ^:dynamic *resource-fn* create-zip-resources)

(defn- create-instances
  ([]
   (create-instances *resource-fn*))
  ([resources-fn]
   (let [res (resources-fn)
         doc-types (->> res (map :doc-type) distinct sort)]
    (->> res
         (#(w/instances "doc-classify" %
                        {:text 'string}
                        [:doc-type doc-types]))
         w/word-count-instances))))

;(->> (create-instances) .numInstances)

(defn- write-arff
  ([]
   (write-arff (create-instances)))
  ([inst]
   (binding [cl/*arff-file* (io/file "/d/a.arff")]
     (cl/write-arff inst))))

(defn- cross-validate []
  (binding [cl/*get-data-fn* #(create-instances)]
    (cl/cross-validate-tests (new-classifier) nil)))

(defn- train-classifier []
  (binding [cl/*get-data-fn* #(create-instances)]
    (cl/train-classifier (new-classifier) nil)))

(defn- get-classifier
  ([] (get-classifier false))
  ([reset?]
   (if reset? (reset! classifier-inst nil))
   (swap! classifier-inst #(or % (train-classifier)))))

(defn- classify [text]
 (let [inst (w/instances "doc-classify"
                         [{:text text}]
                         {:text 'string}
                         [:doc-type (->> (create-zip-resources)
                                         (map :doc-type)
                                         distinct
                                         sort)])]
   (->> inst
        w/word-count-instances
        (#(.instance % 0))
        (.classifyInstance (get-classifier))
        (.value (.classAttribute inst)))))

;(println (get-classifier true))
;(println (get-classifier))
;(do (get-classifier true) nil)
;(println (get-classifier))
;(write-arff)
;(classify "whales whale whale whale whale")
