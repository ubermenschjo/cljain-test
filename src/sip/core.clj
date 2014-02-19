(ns sip.core
  (:require [cljain.dum :refer :all]
            [cljain.sip.core :as sip]
            [cljain.sip.header :as header]
            [cljain.sip.address :as addr])
  (:import [javax.sip.header WWWAuthenticateHeader AuthorizationHeader]
           [javax.sip.message Response]))

(def cr (str "\r\n"))
(def tmp-sdp (.getBytes (str "v=0" cr
                             "o=alice 0 0 IN IP4 localhost" cr
                             "s=-" cr
                             "c=IN IP4 localhost" cr
                             "t=0 0" cr
                             "m=audio 51372 RTP/AVP 0" cr
                             "a=rtpmap:0 PCMU/8000") "UTF-8"))

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(defn init []
  (global-set-account {:user "alice", :domain "localhost", :display-name "Alice", :password "thepwd"})
  (sip/global-bind-sip-provider! (sip/sip-provider! "callee" "127.0.0.1" 5060 "udp"))
  (sip/set-listener! (dum-listener))
  (sip/start!))

(defn release []
  (sip/stop-and-release!))

(defmethod handle-request :REGISTER [request transaction _]
  (if (.getHeader request AuthorizationHeader/NAME)
    (send-response! Response/OK :in transaction)
    (send-response! Response/UNAUTHORIZED :in transaction
                    :more-headers [(header/www-authenticate "Digest" "localhost" "aa2f052b75d9ed32"
                                                            :algorithm "MD5" :stale false)])))

(defmethod handle-request :INVITE [request transaction _]
  (send-response! 200 :in transaction
                  :pack {:type :application
                         :sub-type :sdp
                         :content tmp-sdp}
                  :more-headers [(header/contact (addr/address "sip:alice@127.0.0.1:5060"))]))

(defmethod handle-request :BYE [request transaction _]
  (send-response! 200 :in transaction))
