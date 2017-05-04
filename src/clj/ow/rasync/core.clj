(ns ow.rasync.core
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as p]
            [org.httpkit.server :as s]
            [http.async.client :as c])
  (:import [clojure.lang IDeref]))

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


(defn chan-srv []
  (let [rch (a/chan)
        sch (a/chan)
        handler (fn [req]
                  (s/with-channel req channel
                    (s/on-receive channel
                                  (fn [data]
                                    (println "srv got message:" data)
                                    (a/put! rch data)))
                    ;;;(s/send! channel "startfoo")
                    (a/go-loop [msg (a/<! sch)]
                      (when-not (nil? msg)
                        (println "srv send message:" msg)
                        (s/send! channel msg)
                        (recur (a/<! sch))))))]
    (s/run-server handler {:port 8893})
    {:recv-ch rch
     :send-ch sch}))

(defn chan-clt [url]
  (let [rch (a/chan)
        sch (a/chan)
        handler (fn [ws msg]
                  (println "client got message:" msg)
                  (a/put! rch msg))]
    (let [client (c/create-client)
          ws (c/websocket client
                          url
                          :text handler
                          :error (fn [ws e] (println "ERROR:" e)))]
      (c/send ws :text "startbar")
      (a/go-loop [msg (a/<! sch)]
        (when-not (nil? msg)
          (println "client send message:" msg)
          (c/send ws :text msg)
          (recur (a/<! sch))))
      {:recv-ch rch
       :send-ch sch
       :client client})))

(comment

  (def s1 (chan-srv))
  (def c1 (chan-clt "ws://localhost:8893/ws"))

  (a/put! (:send-ch c1) "cmsg1")
  (a/take! (:recv-ch s1) println)
  (a/put! (:send-ch s1) "smsg1")
  (a/take! (:recv-ch c1) println)

  )
