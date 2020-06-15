(ns tuftedash.app
  (:require
   [reitit.ring :as ring]
   [ring.util.response :as response]
   [env :refer [env]]
   ;; [clj-http.client :as client]
   [clj-http.lite.client :as client]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]
   [reitit.core :as r]
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io])
  (:use hiccup.core
        hiccup.page
        hiccup.form))

(def all-requests {})

(defn req [req]
  (let [res (client/get "http://localhost/listings/17592187574877")]
    (response/response
     (with-out-str
       (clojure.pprint/pprint {:time (:request-time res)
                               :body (:body res)})))))


;; (clojure.tools.reader.edn/read-string (:body (client/get "http://localhost/listings/17592187574877")))
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

(defn index [request]
  (response/response
   (html
    [:pre
     (str request)]
    [:h1 "Index"]
    [:div#app
     ;; https://sayartii.com/perf-report
     [:h1 "App element"]]
    (if (= :dev (:build env))
      (include-js "/assets/dev-main.js")
      (include-js "/assets/main.js")))))

(defn hello [req]
  (->
   (response/response "Hello")))

(def handler
  (ring/ring-handler
   (ring/router
    [
     ["/" {:get index}]
     ;; ["/req" {:get req}]
     ["/hello" {:get hello}]
     ["/req" {:get req}]
     ;; ["/*" (ring/create-resource-handler {:root ""})]
     ["/favicon.ico" (ring/create-resource-handler {:root ""})]
     ["/assets/*" (ring/create-resource-handler {:root ""})]]
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))

(defmethod response/resource-data :resource
  [^java.net.URL url]
  (let [conn (.openConnection url)]
    {:content        (.getInputStream conn)
     :content-length (let [len (.getContentLength conn)] (if-not (pos? len) len))}))
