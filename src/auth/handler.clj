(ns auth.handler
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow$Builder ]
           [com.google.api.client.http.apache ApacheHttpTransport$Builder]
           [com.google.api.client.googleapis.auth.oauth2 GoogleIdTokenVerifier$Builder]
           [com.google.api.client.util.store MemoryDataStoreFactory]
           [com.google.api.client.json.jackson2 JacksonFactory])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.pprint]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defonce CLIENT_ID "281796100165-8fjodck6rd1rp95c28ms79jq2ka2i6jg.apps.googleusercontent.com")
(defonce CLIENT_SECRET "qyfnl43FcZMj7bWY2yIOhycp")
(defonce REDIRECT_URI "http://localhost:3000/signin/oauth2")

(def log
  (let [a (agent "")]
    (fn[& msg]
      (send a #(println "************* " (clojure.string/join "" %2)) msg)
      nil)))

(def flow (-> (GoogleAuthorizationCodeFlow$Builder.
               (. (ApacheHttpTransport$Builder.) build)
               (JacksonFactory/getDefaultInstance)
               CLIENT_ID
               CLIENT_SECRET
               ["profile" "openid" "email"])
              (.setCredentialDataStore (. (MemoryDataStoreFactory/getDefaultInstance) getDataStore "test"))
              (.build)))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (context "/signin" []
           (GET "/oauth2" {{:keys [userid] :as session} :session {:keys [code]} :params}
                (log "in oaut2 url looking for user id " userid)
                (clojure.pprint/pprint session)
                (if-let [credentials (when userid (. flow loadCredential userid))]
                  {:status 200
                   :body "Already logged in"}
                  (if code
                    (do
                      (log "Checking code " code)
                      (if-let [token-response (-> flow
                                                  (. newTokenRequest code)
                                                  (. setRedirectUri REDIRECT_URI)
                                                  (. execute))]
                        (let [verifier (-> (GoogleIdTokenVerifier$Builder.
                                            (. (ApacheHttpTransport$Builder.) build)
                                            (JacksonFactory/getDefaultInstance))
                                           (.build))
                              idtoken (. verifier verify (. token-response getIdToken))
                              payload (. idtoken getPayload)]
                          ;;(. flow createAndStoreCredential token-response)))
                          (log (. payload getUserId) " with emsil " (. payload getEmail))
                          (. flow createAndStoreCredential token-response (. payload getEmail))
                          {:status 200
                           :session (assoc session :userid (. payload getEmail))
                           :body "Logged in"})))
                    
                    (let [auth-url (-> (. flow newAuthorizationUrl)
                                       (. setRedirectUri REDIRECT_URI)
                                       (. build))]
                      (log "redirecting to " auth-url)
                      {:status 302
                       :headers {"Location" auth-url}})))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
