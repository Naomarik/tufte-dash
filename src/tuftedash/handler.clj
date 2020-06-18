(ns tuftedash.handler
  (:require [clojure.tools.reader.edn :as edn]
            [env :refer [env]]
            [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [malli.util :as mu]
            [muuntaja.core :as m]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [ring.util.response :as response]
            [tuftedash.app :as app]))

(defn report-meta [req]
  (response/response (str (app/report-meta))))

(defn report [req]
  (let [{{{:keys [start-uuid end-uuid tag]} :query} :parameters} req]
    (response/response (app/render-report {:start-uuid start-uuid
                                           :end-uuid end-uuid
                                           :tag (edn/read-string tag)}))))

(defn index [request]
  (response/response
   (html
    [:head
     (if (= :dev (:build env))
       (include-css "/assets/public/css/app.css")
       (include-css "/assets/app.css"))]
    [:div#app
     [:h1 "Loading..."]]
    (if (= :dev (:build env))
      (include-js "/assets/dev-main.js")
      (include-js "/assets/main.js")))))

(defn hello [req]
  (response/response "Hello"))

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
