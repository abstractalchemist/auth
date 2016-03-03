(ns auth.google.oauth2-callback
  (:refer-clojure)
  (:gen-class :extends com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet
              :init callback-init
              :state state
              :prefx callback-
              :impl-ns auth.google.oauth-impl))
