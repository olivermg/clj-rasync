(ns ow.rasync.client
  (:require [clojure.core.async :as a]
            #_[com.stuartsierra.component :as cmp]
            #_[integrant.core :as ig]
            #_[clojure.core.async.impl.protocols :as p]
            #_[org.httpkit.server :as s]
            [http.async.client :as c]
            [clojure.edn :as edn]
            #_[ow.rasync.config :refer [get-config]]))

(defrecord WebsocketChannelClient [recv-ch send-ch url state ws client reconnect-interval])

(defn start [{:keys [recv-ch send-ch url state ws client reconnect-interval] :as this}]
  (if (not= @state :started)
    (let [handler (fn [ws msg]
                    (let [msg (edn/read-string msg)]
                      (println "client got message:" msg)
                      (a/put! recv-ch msg)))
          onclose (fn [ws code reason]
                    (println "ON CLOSE" code reason)
                    (when (not= @state :stopped)
                      (a/put! send-ch ::reconnect)))
          onerror (fn [ws e]
                    (println "ON ERROR" e)
                    (when (not= @state :stopped)
                      (a/put! send-ch ::reconnect)))
          connect! (fn []
                     (let [client* (try
                                     (c/create-client)
                                     (catch Exception e
                                       (println "could not create client")))
                           ws* (try
                                 (c/websocket client*
                                              url
                                              :text handler
                                              :close onclose
                                              :error onerror)
                                 (catch Exception e
                                   (println "could not create websocket")
                                   (.close client*)
                                   (Thread/sleep reconnect-interval)))]
                       (reset! client client*)
                       (reset! ws ws*)))]
      (reset! state :started)
      (connect!)
      (let [r (rand)]
        (a/go-loop [msg (a/<! send-ch)]
          (case msg
            nil (println "client stopped listening on send-ch" r)
            ::stop (println "client stopped listening on send-ch" r)
            ::reconnect (do (connect!)
                            (recur (a/<! send-ch)))
            (let [msg (pr-str msg)]
              (println "client send message:" msg r)
              (c/send @ws :text msg)
              (recur (a/<! send-ch))))))
      (println "started client")
      this)
    this))

(defn stop [{:keys [send-ch state ws client] :as this}]
  (if (not= @state :stopped)
    (do (reset! state :stopped)
        (a/put! send-ch ::stop)
        (some-> @ws .close)
        (some-> @client .close)
        (println "stopped client")
        (reset! client nil)
        (reset! ws nil)
        this)
    this))

(defn websocket-channel-client [recv-ch send-ch url & {:keys [reconnect-interval]}]
  (map->WebsocketChannelClient {:recv-ch recv-ch
                                :send-ch send-ch
                                :url url
                                :client (atom nil)
                                :ws (atom nil)
                                :state (atom :stopped)
                                :reconnect-interval (or reconnect-interval 1000)}))
