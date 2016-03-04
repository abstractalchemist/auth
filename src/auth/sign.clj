(ns auth.sign
  (:refer-clojure)
  (:require [clojure.java.io])
  (:import [java.security KeyPairGenerator]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.json.webtoken JsonWebSignature JsonWebSignature$Header JsonWebToken$Payload]))

(def kp (let [key-generator (doto (KeyPairGenerator/getInstance "RSA")
                              (.initialize 2048))]
          (. key-generator genKeyPair)))

(defn write-public
  ""
  []
  (with-open [o (clojure.java.io/output-stream "public_key")]
    (. o write (. kp getPublic))))

(defn json-header
  ""
  [header-map]
  (doto (JsonWebSignature$Header.)
    (.putAll header-map)))

(defn json-payload
  ""
  [payload-map]
  (doto (JsonWebToken$Payload.)
    (.putAll payload-map)))
  

(defn sign-jwt
  ""
  [header payload]
  (let [factory (JacksonFactory/getDefaultInstance)]
    (JsonWebSignature/signUsingRsaSha256
     (. kp getPrivate)
     factory
     (json-header (assoc header "alg" "RS256"))
     (json-payload payload))))

(defn get-jwt
  ""
  [idtoken]
  (let [fac (JacksonFactory/getDefaultInstance)
        jwt (JsonWebSignature/parse fac idtoken)]
    (when (. jwt verify (. kp getPublic))
      (. jwt getPayload))))
