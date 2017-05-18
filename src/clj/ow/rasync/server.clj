(ns ow.rasync.server
  (:require [clojure.core.async :as a]
            #_[com.stuartsierra.component :as cmp]
            #_[integrant.core :as ig]
            #_[clojure.core.async.impl.protocols :as p]
            [org.httpkit.server :as s]
            #_[http.async.client :as c]
            [clojure.edn :as edn]
            #_[ow.rasync.config :refer [get-config]]))

(defrecord WebsocketChannelServer [on-connect port state
                                   server])

(defn start [{:keys [on-connect port state] :as this}]
  (if (not= @state :started)
    (let [handler (fn [req]
                    (let [r (rand)
                          rch (a/chan)
                          sch (a/chan)]
                      (s/with-channel req channel
                        (println "client connected via channel" channel)
                        (s/on-receive channel
                                      (fn [data]
                                        (let [data (edn/read-string data)]
                                          (println "srv got message:" data)
                                          (a/put! rch data))))
                        (s/on-close channel
                                    (fn [status]
                                      (println "srv close" status)
                                        ;;;(when (not= @state :stopped)) ;; only when close comes from client
                                        ;;;(a/put! sch ::stop)
                                      (a/close! rch)
                                      (a/close! sch)))
                        (a/go-loop [msg (a/<! sch)]
                          (if-not (or (nil? msg)
                                      #_(= msg ::stop))
                            (let [msg (pr-str msg)]
                              (println "srv send message:" msg r)
                              (s/send! channel msg)
                              (recur (a/<! sch)))
                            (println "server stopped listening on send-ch")))
                        (on-connect rch sch))))
          _ (reset! state :started)
          server (s/run-server handler {:port port})]
      (println "started server")
      (assoc this :server server))
    this))

(defn stop [{:keys [state server] :as this}]
  (if (not= @state :stopped)
    (do (reset! state :stopped)
        #_(a/put! sch ::stop)
        (server :timeout 20000)
        (println "stopped server")
        (assoc this :server nil))
    this))

(defn websocket-channel-server [on-connect & {:keys [port]}]
  {:pre [(fn? on-connect)]}
  (map->WebsocketChannelServer {:on-connect on-connect
                                :port (or port 8899)
                                :state (atom :stopped)}))
