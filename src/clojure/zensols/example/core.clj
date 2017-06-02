(ns zensols.example.core
  (:require [zensols.actioncli.parse :as cli]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.resource :as res]
            [zensols.model.weka])
  (:gen-class :main true))

(defn- create-command-context []
  {:command-defs '((:repl zensols.actioncli repl repl-command)
                   (:load-corpora zensols.example anon-db load-corpora-command)
                   (:classify zensols.example sa-model classify-utterance-command))})

(defn -main [& args]
  (lu/configure "nlp-ml-log4j.xml")
  (let [command-context (create-command-context)]
    (cli/process-arguments command-context args)))
