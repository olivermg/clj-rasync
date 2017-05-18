(ns user
  (:require [clojure.core.async :as a]
            [ow.rasync.server :as s]
            [ow.rasync.client :as c]
            #_[clojure.edn :as edn]
            #_[clojure.java.io :as io]
            #_[ow.rasync.config :as cfg]))

#_(defn- read-config-local []
  (cfg/read-config "dev.edn"))

#_(defn- read-config-dev []
  (cfg/read-config "local.edn"))

#_(defn read-config []
  (merge (cfg/read-config)
         (read-config-dev)
         (read-config-local)))
