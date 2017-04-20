(ns zensols.example.sa-tp-feature
  (:require [clojure.tools.logging :as log])
  (:require [zensols.nlparse.parse :as p]
            [zensols.util.string :as zs]
            [zensols.nlparse.feature.lang :as fe]
            [zensols.nlparse.feature.word :as fw]
            [zensols.nlparse.feature.word-count :as wc]
            [zensols.example.anon-db :as adb]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec]))

(def ^{:dynamic true :private true} *context* nil)

(defn- context []
  (or *context*
      (throw (ex-info "No context bound" {}))))

(defmacro with-context
  {:style/indent 1}
  [context & forms]
  `(binding [*context* ~context]
     ~@forms))

(defn create-features
  [panon context]
  (log/debugf "creating features (context=<%s>) for <%s>"
              (zs/trunc context) (zs/trunc panon))
  (let [{:keys [word-count-stats]} context
        tokens (p/tokens panon)]
    (merge (fe/verb-features (->> panon :sents first))
           (fw/token-features panon tokens)
           (fe/pos-tag-features tokens)
           (wc/label-count-score-features panon word-count-stats))))

(defn create-feature-sets [& adb-keys]
  (let [context (context)
        {:keys [anons-fn]} context]
   (->> (apply anons-fn adb-keys)
        (map #(merge {:sa (:class-label %)
                      :utterance (->> % :instance :text)}
                     (create-features (:instance %) context))))))

(defn create-context
  [& {:keys [anons-fn]
      :or {anons-fn adb/anons}}]
  (->> (anons-fn)
       wc/calculate-feature-stats
       (hash-map :anons-fn anons-fn :word-count-stats)))

(defn feature-metas []
  (let [{:keys [word-count-stats]} (context)]
    (concat (fe/verb-feature-metas)
            (fw/token-feature-metas)
            (fe/pos-tag-feature-metas)
            (wc/label-word-count-feature-metas word-count-stats))))

(defn- class-feature-meta []
  [:sa ["answer" "question" "expressive"]])

(defn create-model-config []
  {:name "speech-act-two-pass"
   :create-feature-sets-fn create-feature-sets
   :create-features-fn create-features
   :feature-metas-fn feature-metas
   :class-feature-meta-fn class-feature-meta
   :model-return-keys #{:label :distributions :features}})

(defn- main [& actions]
  (with-context (create-context)
    (->> actions
         (map (fn [action]
                (case action
                  1 (with-model-conf (create-model-config)
                      (ec/display-features)))))
         doall)))
