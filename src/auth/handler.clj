(ns auth.handler
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow$Builder ]
           [com.google.api.client.http.apache ApacheHttpTransport$Builder]
           [com.google.api.client.googleapis.auth.oauth2 GoogleIdTokenVerifier$Builder]
           [com.google.api.client.util.store MemoryDataStoreFactory]
           [com.google.api.client.json.jackson2 JacksonFactory])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.pprint]
            [auth.sign]
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
  (context "/signin" []
           (OPTIONS "/oaut2" []
                    {:status 500
                     :body "not implemented"})
           
           (GET "/oauth2" {{:keys [userid redirect_uri] :as session} :session {:strs [Authorization]} :headers {:keys [code redirect]} :params}
                ;;(log "in oaut2 url looking for user id " userid " or auth " Authorization)
                
                (clojure.pprint/pprint session)
                (if-let [credentials (if userid
                                       ;; load via session user id
                                       (. flow loadCredential userid)

                                       ;; else, check for authorization header
                                       (when Authorization
                                         (let [payload (auth.sign/get-jwt (let [[_ token] (clojure.string/split Authorization #"\s")]
                                                                            token))]
                                           (. flow loadCredential
                                              (get payload "id")))))]
                  {:status 200
                   :session (assoc session :redirect_uri redirect)
                   :headers {"Content-Type" "text/html"}
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
                          ;;(log (. payload getUserId) " with emsil " (. payload getEmail))
                          (. flow createAndStoreCredential token-response (. payload getEmail))
                          (if (seq redirect_uri)
                            {:status 302
                             :session (disssoc session :redirect_uri)
                             :header {"Location" redirect_uri}}
                            {:status 200
                             ;;:session (assoc session :userid (. payload getEmail))
                             :headers {"Content-Type" "text/html"
                                       "Authorization" (str "Bearer " (auth.sign/sign-jwt {} (let [start (System/currentTimeMillis)
                                                                                                   end (+ start (* 1000 60 60 1))]
                                                                                               {"id" (. payload getEmail)
                                                                                                "iss" "AbstractAlchemist"
                                                                                                "iat" start
                                                                                                "exp" end })))}
                             :body "Logged in"}))))
                    
                    (let [auth-url (-> (. flow newAuthorizationUrl)
                                       (. setRedirectUri REDIRECT_URI)
                                       (. build))]
                      (log "redirecting to " auth-url)
                      {:status 302
                       :headers {"Location" auth-url}})))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
