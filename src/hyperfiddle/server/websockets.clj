(ns hyperfiddle.server.websockets
  (:require
    ;; [hypercrud.transit :as hc-t] ;; TODO restore
    ;; [hyperfiddle.service.auth :as auth] ;; TODO restore
    [missionary.core :as m]
    [hfdl.impl.util :as u])
  (:import (org.eclipse.jetty.servlet ServletContextHandler ServletHolder)
           (org.eclipse.jetty.websocket.api RemoteEndpoint Session WebSocketConnectionListener
                                            WebSocketListener WriteCallback SuspendToken)
           (org.eclipse.jetty.websocket.servlet WebSocketCreator WebSocketServlet)
           (clojure.lang IFn)
           (java.nio ByteBuffer)))

(defn close! [^Session session]
  (.close session))

(defn open? [^Session session]
  (.isOpen session))

(defn write-str [^RemoteEndpoint remote ^String message]
  (fn [s f]
    (.sendString remote message
      (reify WriteCallback
        (writeFailed [_ e] (f e))
        (writeSuccess [_] (s nil))))
    u/nop))

(defn write-buf [^RemoteEndpoint remote ^ByteBuffer message]
  (fn [s f]
    (.sendBytes remote message
      (reify WriteCallback
        (writeFailed [_ e] (f e))
        (writeSuccess [_] (s nil))))
    u/nop))

(defn session-suspend! [^Session session]
  (.suspend session))

(defn token-resume! [^SuspendToken token]
  (.resume token))

(deftype Ws [boot
             ^:unsynchronized-mutable session
             ^:unsynchronized-mutable msg-str
             ^:unsynchronized-mutable msg-buf
             ^:unsynchronized-mutable close
             ^:unsynchronized-mutable token
             ^:unsynchronized-mutable error]
  IFn
  (invoke [_ _]
    (token-resume! token))
  WebSocketConnectionListener
  (onWebSocketConnect [this s]
    (prn :connect)
    (set! session s)
    (boot (.getRemote s)
      (set! msg-str (m/rdv))
      (set! msg-buf (m/rdv))
      (set! close (m/dfv))))
  (onWebSocketClose [this s r]
    (prn :close)
    (close
      (do
        (set! close nil)
        (set! msg-str nil)
        (set! msg-buf nil)
        (merge {:status s :reason r}
          (when-some [e error]
            (set! error nil)
            {:error e})))))
  (onWebSocketError [this e]
    (prn :error)
    (set! error e))
  WebSocketListener
  (onWebSocketText [this msg]
    (prn :text)
    (set! token (session-suspend! session))
    ((msg-str msg) this u/pst))
  (onWebSocketBinary [this payload offset length]
    (prn :binary)
    (set! token (session-suspend! session))
    ((msg-buf (ByteBuffer/wrap payload offset length)) this u/pst)))

(def add-ws-endpoints
  (partial reduce-kv
    (fn [^ServletContextHandler ctx ^String path handler]
      (doto ctx
        (.addServlet
          (ServletHolder.
            (proxy [WebSocketServlet] []
              (configure [factory]
                (.setCreator factory
                  (reify WebSocketCreator
                    (createWebSocket [_ request response]
                      ;; TODO re-enable auth
                      #_(auth/build-auth-context config request)
                      (when (and
                              #_(auth/configured? context)
                              #_(auth/authenticated? context)
                              )
                        (->Ws (handler request) nil nil nil nil nil nil))))))))
          ^String path)))))