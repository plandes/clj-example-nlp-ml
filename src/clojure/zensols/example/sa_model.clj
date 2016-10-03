(ns zensols.example.sa-model
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer (pprint)])
  (:require [clj-excel.core :as excel])
  (:require [zensols.actioncli.dynamic :refer (dyn-init-var) :as dyn]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.resource :as res]
            [zensols.util.spreadsheet :as ss]
            [zensols.model.execute-classifier :as exc :refer (with-model-conf)]
            [zensols.nlparse.parse :as p]
            [zensols.example.anon-db :as adb])
  (:require [zensols.example.sa-feature :as sf]))

(dyn-init-var *ns* 'model-inst (atom nil))
(dyn-init-var *ns* 'preds-inst (atom nil))

(defn reset-model []
  (reset! model-inst nil)
  (reset! preds-inst nil))

(dyn/register-purge-fn reset-model)

(defn- model []
  (swap! model-inst
         #(or %
              (with-model-conf (sf/create-model-config)
                (exc/prime-model (exc/read-model))))))

(defn- classify-info [anon]
  (exc/classify (model) anon))

(defn- pprint-classify-results [anon]
  (pprint (classify-info anon)))

(defn classify [anon]
  (->> anon
       classify-info
       :label))

(defn classify-utterance [utterance]
  (classify (p/parse utterance)))

(defn- test-annotation [anon-rec]
  (let [{anon :instance label :class-label} anon-rec
        sent (:text anon)
        pred (classify-utterance sent)]
    (log/debugf "label: %s, prediction: %s" label pred)
    {:label label
     :sent sent
     :prediction pred
     :correct? (= label pred)}))

(defn- predict-test-set []
  (swap! preds-inst
         #(or %
              (let [anons (adb/anons :set-type :test)
                    results (map test-annotation anons)
                    preds (map :correct? results)]
                {:correct (filter true? preds)
                 :incorrect (filter false? preds)
                 :predictions preds
                 :results results}))))

(defn- stats []
  (let [{:keys [correct incorrect predictions]} (predict-test-set)
        total (count predictions)
        correct-count (count correct)
        accuracy (/ correct-count total)]
    (println (format "total: %d, correct: %d, accuracy: %.3f"
                     total correct-count
                     (double accuracy)))))

(defn- create-prediction-report []
  (letfn [(data-sheet [anons]
            (->> anons
                 (map (fn [anon]
                        [(:class-label anon) (->> anon :instance :text)]))
                 (cons ["Label" "Utterance"])))]
   (let [out-file (res/resource-path :analysis-report "sa-predictions.xls")]
     (-> (excel/build-workbook
          (excel/workbook-hssf)
          {"Predictions on test data"
           (->> (predict-test-set)
                :results
                (map (fn [res]
                       (let [{:keys [label sent prediction correct?]} res]
                         [correct? label prediction sent])))
                (cons ["Is Correct" "Gold Label" "Prediction" "Utterance"])
                (ss/headerize))
           "Training" (data-sheet (adb/anons))
           "Test" (data-sheet (adb/anons :set-type :test))})
         (ss/autosize-columns)
         (excel/save out-file)))))

(defn- main [& actions]
  (let [title "when do you want to go"]
    (->> (map (fn [action]
                (case action
                  -1 title
                  0 (dyn/purge)
                  1 (reset-model)
                  p (exc/print-model-info (model))
                  2 (exc/dump-model-info (model))
                  3 (pprint-classify-results (p/parse title))
                  4 (:label (classify-info (p/parse title)))
                  5 (classify (p/parse title))
                  6 (stats)
                  7 (->> (p/parse title)
                         (classify-info)
                         (#(select-keys % [:label :distributions]))
                         pprint)
                  8 (create-prediction-report)))
              actions)
         doall)))

(def classify-utterance-command
  "CLI command to classify an utterance as a question."
  {:description "parse an English utterance"
   :options
   [(lu/log-level-set-option)
    ["-u" "--utterance" "The utterance to parse"
     :required "TEXT"
     :validate [#(> (count %) 0) "No utterance given"]]]
   :app (fn [{:keys [utterance] :as opts} & args]
          (println (classify-utterance utterance)))})

