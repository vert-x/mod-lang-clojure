(ns vertx.eventbus-test
  (:require [vertx.testtools :as t]
            [vertx.core :as core]
            [vertx.eventbus :as eb]))

(defn test-send []
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        id (atom nil)]
    (reset! id
            (eb/register-handler
             addr
             (fn [m]
               (t/test-complete
                (t/assert= msg (eb/message-body m))
                (eb/unregister-handler @id)))))
    (t/assert-not-nil @id)
    (eb/send addr msg)))

(defn test-reply []
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        reply {:biscuit "gravy"}
        id (atom nil)]
    (reset! id
            (eb/register-handler
             addr
             (fn [m]
               (t/test-complete
                (t/assert= msg (eb/message-body m))
                (eb/reply m reply)))))
    
    (t/assert-not-nil @id)
    (eb/send addr msg
             (fn [m]
               (t/test-complete
                (t/assert= reply (eb/message-body m))
                (eb/unregister-handler @id))))))

(defn test-send-unregister-send []
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        id (atom nil)
        rcvd (atom false)]
    (reset! id
            (eb/register-handler
             addr
             (fn [m]
               (if @rcvd
                 (throw (IllegalStateException. "Handler already called")))
               (t/assert= msg (eb/message-body m))
               (eb/unregister-handler @id)
               (reset! rcvd true)
               (core/timer 100 (t/test-complete)))))
    
    (t/assert-not-nil @id)
    (dotimes [_ 2]
      (eb/send addr msg))))

(defn test-publish-multiple-matching-handlers []
  (let [msg {:ham "biscuit"}
        addr "eb.test"
        total 10
        count (atom 1)]
    (dotimes [_ total]
      (let [id (atom nil)]
        (reset! id
                (eb/register-handler
                 addr
                 (fn [m]
                   (t/assert= msg (eb/message-body m))
                   (eb/unregister-handler @id)
                   (swap! count inc)
                   (if (= @count total)
                     (t/test-complete)))))))
    
    (eb/publish addr msg)))

(defn test-reply-of-reply-of-reply []
  (let [addr "eb.test"
        id (atom nil)]
    (reset! id
            (eb/register-handler
             addr
             (fn [m]
               (t/test-complete
                (t/assert= "message" (eb/message-body m))
                (eb/reply m "reply"
                          (fn [m]
                            (t/assert= "reply-of-reply" (eb/message-body m))
                            (eb/reply m "reply-of-reply-of-reply")))))))

    (eb/send addr "message"
             (fn [m]
               (t/assert= "reply" (eb/message-body m))
               (eb/reply m "reply-of-reply"
                         (fn [m]
                           (t/test-complete
                            (t/assert "reply-of-reply-of-reply")
                            (eb/unregister-handler @id))))))))

(defn test-message-types-roundtrip []
  (let [addr "eb.test"
        tfn
        (fn [msg]
          (let [id (atom nil)]
            (reset! id
                    (eb/register-handler
                     addr
                     (fn [m]
                       (eb/unregister-handler @id)
                       (eb/reply m (eb/message-body m)))))
    
            (eb/send addr msg
                     (fn [m]
                       (t/test-complete
                        (t/assert= msg (eb/message-body m)))))))]
    (doseq [m ["ham"
               nil
               true
               false
               1
               1.1
               [1 2 3]
               [{:a "b"} 2]
               {:a "biscuit" :b nil :c true :d false :e 1 :f 1.1 :g [1 2 3]}]]
      (tfn m))))

(t/start-tests)
