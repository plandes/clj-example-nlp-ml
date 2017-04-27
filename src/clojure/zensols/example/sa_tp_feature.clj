(ns ^{:doc "Just like [[zensols.example.sa_feature]] but this shows how to use a
two pass cross validation.  See [[zensols.example.sa-tp-eval]]."
      :author "Paul Landes"}
    zensols.example.sa-tp-feature
  (:require [clojure.tools.logging :as log])
  (:require [zensols.nlparse.parse :as p]
            [zensols.util.string :as zs]
            [zensols.nlparse.feature.lang :as fe]
            [zensols.nlparse.feature.word :as fw]
            [zensols.nlparse.feature.word-count :as wc]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec]))

(def id-key :id)

(def ^{:dynamic true :private true} *context* nil)

(def ^:private classes ["answer" "question" "expressive"])

(defn- context
  [& {:keys [no-throw?]
      :or {no-throw? false}}]
  (or *context*
      (and (not no-throw?)
           (throw (ex-info "No context bound" {})))))

(defmacro with-feature-context
  {:style/indent 1}
  [context & forms]
  `(binding [*context* ~context]
     ~@forms))

(defn create-features
  ([panon]
   (create-features panon nil))
  ([panon context]
   (log/debugf "creating features (context=<%s>) for <%s>"
               (zs/trunc context) (zs/trunc panon))
   (let [{:keys [word-count-stats]} context
         tokens (p/tokens panon)]
     (merge (fe/verb-features (->> panon :sents first))
            (fw/token-features panon tokens)
            (fe/pos-tag-features tokens)
            (if word-count-stats
              (wc/label-count-score-features panon word-count-stats))))))

(defn- flatten-keys [adb-keys]
  (mapcat #(into [] %) adb-keys))

(defn create-feature-sets [& {:keys [context] :as adb-keys}]
  (log/infof "creating features with keys=%s: %s"
             adb-keys (zs/trunc adb-keys))
  (let [context (or context *context*)
        {:keys [anons-fn]} context
        anons (apply anons-fn (->> (flatten-keys adb-keys)
                                   (concat [:include-ids? true])))
        ;; we must provide keys so the second pass can correlate results back
        ;; in the testing/training for each fold; the label is also needed for
        ;; every instance
        fs (if (ec/executing-two-pass?)
             (->> anons
                  (map (fn [{:keys [id class-label]}]
                         {:sa class-label
                          id-key id})))
             (->> anons
                  (map (fn [{:keys [class-label instance id]}]
                         (merge {:sa class-label
                                 :utterance (->> instance :text)
                                 id-key id}
                                (create-features instance context))))))]
    (log/debugf "ids: %s" (->> fs (map #(-> % id-key read-string)) pr-str))
    fs))

(defn create-context
  [& {:keys [anons-fn] :as adb-keys}]
  (let [fkeys (flatten-keys adb-keys)
        anons (apply anons-fn fkeys)]
    (log/infof "creating context with key=%s anon count: %d"
               (zs/trunc adb-keys) (count anons))
    (log/tracef "adb-keys: %s" (pr-str adb-keys))
    (->> anons
         wc/calculate-feature-stats
         (hash-map :anons-fn anons-fn :word-count-stats))))

(defn feature-metas [& _]
  (concat (fe/verb-feature-metas)
          (fw/token-feature-metas)
          (fe/pos-tag-feature-metas)
          (wc/label-word-count-feature-metas classes)))

(defn- class-feature-meta []
  [:sa classes])

(defn create-model-config []
  {:name "speech-act-two-pass"
   :create-feature-sets-fn create-feature-sets
   :create-features-fn create-features
   :feature-metas-fn feature-metas
   :class-feature-meta-fn class-feature-meta
   :create-two-pass-context-fn create-context
   :model-return-keys #{:label :distributions :features}})

(defn- main [& actions]
  (with-feature-context (create-context)
    (->> actions
         (map (fn [action]
                (case action
                  1 (with-model-conf (create-model-config)
                      (ec/display-features :max 10)))))
         doall)))
