(ns tuftedash.app
  (:require
   [reitit.ring :as ring]
   [ring.util.response :as response]
   ;; [ring.middleware.reload :refer [wrap-reload]]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]
   [reitit.core :as r]
   [clojure.java.io :as io])
  (:use hiccup.core
        hiccup.page
        hiccup.form))

;; (def router
;;   (r/router
;;    [["/api/ping" ::ping]
;;     ["/api/orders/:id" ::order]]))

;; (r/match-by-path router "/api/ping")
;;                                         ; #Match{:template "/api/ping"
;;                                         ;        :data {:name ::ping}
;;                                         ;        :result nil
;;                                         ;        :path-params {}
;;                                         ;        :path "/api/ping"}

;; (r/match-by-name router ::order {:id 2})
;;                                         ; #Match{:template "/api/orders/:id",
;;                                         ;        :data {:name ::order},
;;                                         ;        :result nil,
;;                                         ;        :path-params {:id 2},
;;                                         ;        :path "/api/orders/2"}

;; (defn html-response
;;   ""
;;   [data]
;;   (-> (response/response data)
;;       (response/header "content-type" "text/html")
;;       )
;;   )

(defn testz [request]
  (response/response
   (html
    [:pre
     (str request)]
    (include-js "/assets/test.js")
    ;; (include-js (slurp (io/resource "test.js")))
    [:h1 "TeST"])))

(defn two [request]
  (response/response
   (html
    [:pre
     (str request)]
    (include-js "/assets/test.js")
    ;; (include-js (slurp (io/resource "test.js")))
    [:h1 "twoST"])))

(def -handler
  (ring/ring-handler
   (ring/router
    [
     ["/" {:get testz}]
     ["/two" {:get two}]
     ["/assets/*" (ring/create-resource-handler {:root ""})]
     ]
    ;; router data effecting all routes
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))


;; (def handler (wrap-reload #'-handler))
(def handler -handler)
;; (def handler hello)
