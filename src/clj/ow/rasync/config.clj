(ns ow.rasync.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def config (-> "ow/rasync/config.edn"
                io/resource
                slurp
                edn/read-string))
