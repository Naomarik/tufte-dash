(ns tuftedash.handler
  (:require
   [tuftedash.app :as app]
   [muuntaja.core :as m]
   [taoensso.encore :as encore]
   [malli.util :as mu]
   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.coercion :as coercion]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   [reitit.coercion.malli :as malli-coercion]
   [tuftedash.utils :as utils]
   [ring.util.response :as response]
   [env :refer [env]]
   ;; [clj-http.client :as client]
   [clj-http.lite.client :as client]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]
   [reitit.core :as r]
   [clojure.tools.reader.edn :as edn]
   [mount.core :as mount])
  (:use hiccup.core
        hiccup.page
        hiccup.form))

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



(defn report-meta [req]
  (response/response (str (app/meta))))

(defn report [req]
  (let [{{{:keys [start-uuid end-uuid tag]} :query} :parameters} req]
    (response/response (app/render-report {:start-uuid start-uuid
                                           :end-uuid end-uuid
                                           :tag (edn/read-string tag)}))))

(defn index [request]
  (response/response
   (html
    [:head
     (include-css "/assets/public/css/app.css")]
    #_[:pre
     (str request)]
    ;; [:h1 "Index"]
    ;; [:h1 "PStats Count: " (count @reports)]
    [:div#app
     [:h1 "Loading..."]]
    (if (= :dev (:build env))
      (include-js "/assets/dev-main.js")
      (include-js "/assets/main.js")))))

(defn hello [req]
  (response/response "Hello"))
;; http://recharts.org/en-US/api/ComposedChart

;; (defn match-by-path-and-coerce! [path]
;;   (if-let [match (r/match-by-path router path)]
;;     (assoc match :parameters (coercion/coerce! match))))

(def handler
  (ring/ring-handler
   (ring/router
    [
     ["/" {:get index}]
     ["/hello" {:get hello}]
     ["/report-meta" {:get report-meta}]
     ["/report" {:get report
                 :parameters {:query [:map
                                      [:start-uuid
                                       {:optional true}
                                       string?]
                                      [:end-uuid {:optional true}
                                       string?]
                                      [:tag {:optional true}
                                       string?]]}}]
     ;; ["/*" (ring/create-resource-handler {:root ""})]
     ["/favicon.ico" (ring/create-resource-handler {:root ""})]
     ["/assets/*" (ring/create-resource-handler {:root ""})]]
    {:data {
            :muuntaja m/instance

            ;; :coercion reitit.coercion.spec/coercion
            :coercion
            (reitit.coercion.malli/create
             {;; set of keys to include in error messages
              :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
              ;; schema identity function (default: close all map schemas)
              :compile mu/open-schema
              ;; strip-extra-keys (effects only predefined transformers)
              :strip-extra-keys true
              ;; add/set default values
              :default-values true
              ;; malli options
              :options nil})
            :middleware [

                         parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware


                         rrc/coerce-exceptions-middleware
                         rrc/coerce-response-middleware
                         rrc/coerce-request-middleware
                         ]}})))

(defmethod response/resource-data :resource
  [^java.net.URL url]
  (let [conn (.openConnection url)]
    {:content        (.getInputStream conn)
     :content-length (let [len (.getContentLength conn)] (if-not (pos? len) len))}))
