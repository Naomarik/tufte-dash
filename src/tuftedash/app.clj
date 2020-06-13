(ns tuftedash.app
  (:require
   [reitit.ring :as ring]
   [ring.util.response :as response]
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

(defn testz [request]
  (response/response
   (html
    [:pre
     (str request)]
    (include-js "/assets/test.js")
    ;; (include-js (slurp (io/resource "test.js")))
    [:h1 "TeS"])))

(defn two [request]
  (def ahedkrj request)
  (response/response
   (html
    ;; (include-js "/assets/main.js")
    [:pre
     (str request)]
    [:h1 "ttesST"]

    ;; (include-js "/assets/cljs-out/dev-main.js")
    (include-js "/assets/main.js")
    )))
;; (slurp (io/resource "cljs-out/dev-main.js"))

(defmethod response/resource-data :resource
  [^java.net.URL url]
  (let [conn (.openConnection url)]
    {:content        (.getInputStream conn)
     :content-length (let [len (.getContentLength conn)] (if-not (pos? len) len))}))

(def handler
  (ring/ring-handler
   (ring/router
    [
     ["/" {:get testz}]
     ["/two" {:get two}]
     ;; ["/*" (ring/create-resource-handler {:root ""})]
     ["/favicon.ico" (ring/create-resource-handler {:root ""})]
     ["/assets/*" (ring/create-resource-handler {:root ""})]
     ]
    ;; router data effecting all routes
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))


;; (def handler hello)

