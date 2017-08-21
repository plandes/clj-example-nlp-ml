(ns zensols.example.nlp-ml-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log])
  (:require [zensols.actioncli.resource :as res :refer (with-resources)])
  (:require [zensols.example.anon-db :as db]
            [zensols.example.sa-eval :as eval]
            [zensols.example.sa-model :as model]))

(defn load-corpora []
  (if (= 0 (count (db/anons)))
    (db/load-corpora))
  ;; elastic search eventual consistency
  (Thread/sleep 3000))

(defmacro with-test-resources
  {:style/indent 0}
  [& forms]
  `(let [f# "target"]
     (res/register-resource :model-read :system-file f#)
     (res/register-resource :model-write :system-file f#)
     ~@forms))

(deftest nlp-ml
  (testing "nlp features with ml algos"
    (with-test-resources
      (load-corpora)
      (eval/main 'wm)
      (is (= 477 (count (db/anons))))
      (is (= "question" (model/classify-utterance "Why do we have to go to the store")))
      (is (= "answer" (model/classify-utterance "Because we need food")))
      (is (= "expressive" (model/classify-utterance "Good"))))))
