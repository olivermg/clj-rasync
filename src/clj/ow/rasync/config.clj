(ns ow.rasync.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defonce ^:private +config+ (atom nil))

(defn read-config []
  (some-> "ow/rasync/config.edn"
          io/resource
          slurp
          edn/read-string))

(defn get-config []
  (if-not (nil? @+config+)
    @+config+
    (reset! +config+ (read-config))))
