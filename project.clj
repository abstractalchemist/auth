(defproject auth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.google.api-client/google-api-client "1.20.0"]
                 [com.google.oauth-client/google-oauth-client-servlet "1.20.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :aot [auth.google.oauth2 auth.google.oauth2-callback]
  :ring {:handler auth.handler/app
         :web-xml "web.xml"}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
