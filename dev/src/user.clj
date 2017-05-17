(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ow.rasync.config :as cfg]))

(defn- read-dev-config []
  (some-> "dev.edn"
          io/resource
          slurp
          edn/read-string))

(defn read-config []
  (merge (cfg/read-config)
         (read-dev-config)))
