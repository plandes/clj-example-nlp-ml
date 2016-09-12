(ns zensols.example.sa-feature
  (:require [clojure.tools.logging :as log])
  (:require [zensols.nlparse.parse :as p]
            [zensols.nlparse.feature.lang :as fe]
            [zensols.example.anon-db :as adb]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec]))

(defn create-features
  ([panon]
   (create-features panon nil))
  ([panon context]
   (log/debugf "creating features (context=<%s>) for <%s>" context panon)
   (let [tokens (p/tokens panon)]
     (merge (fe/verb-features (->> panon :sents first))
            (fe/token-features panon tokens)
            (fe/pos-tag-features tokens)
            (fe/dictionary-features tokens)
            (fe/tree-features panon)
            (fe/srl-features tokens)))))

(defn create-feature-sets [& adb-keys]
  (->> (apply adb/anons adb-keys)
       (map #(merge {:sa (:class-label %)
                     :utterance (->> % :instance :text)}
                    (create-features (:instance %))))))

(defn feature-metas []
  (concat (fe/verb-feature-metas)
          (fe/token-feature-metas)
          (fe/pos-tag-feature-metas)
          (fe/dictionary-feature-metas)
          (fe/tree-feature-metas)
          (fe/srl-feature-metas)))

(defn- class-feature-meta []
  [:sa ["answer" "question" "expressive"]])

(defn create-model-config []
  {:name "speech-act"
   :create-feature-sets-fn create-feature-sets
   :create-features-fn create-features
   :feature-metas-fn feature-metas
   :class-feature-meta-fn class-feature-meta
   :model-return-keys #{:label :distributions :features}})

(defn- main [& actions]
  (->> actions
       (map (fn [action]
              (case action
                1 (with-model-conf (create-model-config)
                    (ec/display-features)))))
       doall))
