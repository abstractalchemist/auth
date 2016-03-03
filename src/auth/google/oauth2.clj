(ns auth.google.oauth2
  (:refer-clojure)
  (:gen-class :extends com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeServlet
              :init base-init
              :state state
              :impl-ns auth.google.oauth-impl
              :prefix base-))
