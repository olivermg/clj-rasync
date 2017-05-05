(ns ow.rasync.core
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as cmp]
            #_[clojure.core.async.impl.protocols :as p]
            [org.httpkit.server :as s]
            [http.async.client :as c])
  #_(:import [clojure.lang IDeref]))

#_(defn- box [v]
  (reify IDeref
    (deref [_] v)))

#_(deftype ServerChan [channel]
  p/ReadPort
  (take! [port fn1-handler]
    (box 11))

  p/WritePort
  (put! [port val fn1-handler]
    (box true))

  p/Channel
  (close! [chan]
    true)
  (closed? [chan]
    false))

(defrecord WebsocketChannelServer [recv-ch send-ch
                                   server state]

  cmp/Lifecycle

  (start [this]
    (if (not= @state :started)
      (let [handler (fn [req]
                      (let [r (rand)]
                        (s/with-channel req channel
                          (println "client connected via channel" channel)
                          (s/on-receive channel
                                        (fn [data]
                                          (println "srv got message:" data)
                                          (a/put! recv-ch data)))
                          (s/on-close channel
                                      (fn [status]
                                        (println "srv close" status)
                                        (when (not= @state :stopped) ;; only when close comes from client
                                          (a/put! send-ch ::stop))))
                          (a/go-loop [msg (a/<! send-ch)]
                            (if-not (or (nil? msg)
                                        (= msg ::stop))
                              (do (println "srv send message:" msg r)
                                  (s/send! channel msg)
                                  (recur (a/<! send-ch)))
                              (println "server stopped listening on send-ch"))))))
            _ (reset! state :started)
            server (s/run-server handler {:port 8897})]
        (println "started server")
        (assoc this :server server))
      this))

  (stop [this]
    (if (not= @state :stopped)
      (do (reset! state :stopped)
          (a/put! send-ch ::stop)
          (server :timeout 20000)
          (println "stopped server")
          (assoc this :server nil))
      this)))

(defn websocket-channel-server []
  (map->WebsocketChannelServer {:recv-ch (a/chan)
                                :send-ch (a/chan)
                                :state (atom :stopped)}))

(defrecord WebsocketChannelClient [recv-ch send-ch url
                                   client ws state]

  cmp/Lifecycle

  (start [this]
    (if (not= @state :started)
      (let [handler (fn [ws msg]
                      (println "client got message:" msg)
                      (a/put! recv-ch msg))
            onclose (fn [ws code reason]
                      (println "ON CLOSE" code reason)
                      (when (not= @state :stopped)
                        (a/put! send-ch ::reconnect)))
            onerror (fn [ws e]
                      (println "ON ERROR" e)
                      (when (not= @state :stopped)
                        (Thread/sleep 5000)
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
                                     (.close client*)))]
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
              (do (println "client send message:" msg r)
                  (c/send @ws :text msg)
                  (recur (a/<! send-ch))))))
        (println "started client")
        this)
      this))

  (stop [this]
    (if (not= @state :stopped)
      (do (reset! state :stopped)
          (a/put! send-ch ::stop)
          (.close @ws)
          (.close @client)
          (println "stopped client")
          (reset! client nil)
          (reset! ws nil)
          this)
      this)))

(defn websocket-channel-client [url]
  (map->WebsocketChannelClient {:recv-ch (a/chan)
                                :send-ch (a/chan)
                                :url url
                                :client (atom nil)
                                :ws (atom nil)
                                :state (atom :stopped)}))


(comment

  (def s1 (websocket-channel-server))
  (def c1 (websocket-channel-client "ws://localhost:8897/ws"))
  (def c2 (websocket-channel-client "ws://localhost:8897/ws"))

  (def s1 (cmp/start s1))
  (def c1 (cmp/start c1))
  (def c2 (cmp/start c2))

  (a/put! (:send-ch c1) "cmsg1")
  (a/take! (:recv-ch s1) println)
  (a/put! (:send-ch s1) "smsg1")
  (a/take! (:recv-ch c1) println)

  (a/put! (:send-ch c2) "cmsg2")
  (a/take! (:recv-ch s1) println)
  (a/put! (:send-ch s1) "smsg2")
  (a/take! (:recv-ch c2) println)

  (def c2 (cmp/stop c2))
  (def c1 (cmp/stop c1))
  (def s1 (cmp/stop s1))

  )
