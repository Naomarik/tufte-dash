(ns app
  (:require
   [reitit.ring :as ring]
   [ring.middleware.reload :refer [wrap-reload]]
   [mount.core :refer [defstate] :as mount]
   [nrepl.server :as nrepl-server]
   [cider.nrepl :refer (cider-nrepl-handler)]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]
   [reitit.core :as r]
   [clojure.tools.cli :refer [parse-opts]]
   [ring.adapter.jetty :refer [run-jetty]]))

(def routes
  (ring/ring-handler
   (ring/router
    ["/" {:get hello}]
    ;; router data effecting all routes
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))
