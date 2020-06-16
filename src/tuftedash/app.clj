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
   [taoensso.nippy :as nippy]
   [taoensso.tufte :as tufte :refer [defnp p profiled profile]]
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [taoensso.tufte.impl PData PStats PState TimeSpan Time]
   java.io.ByteArrayOutputStream)
  (:use hiccup.core
        hiccup.page
        hiccup.form))

(defprotocol tufte-types->data
  (->data [x]))

(extend-protocol tufte-types->data
  clojure.lang.Volatile
  (->data [x]
    (->data @x))

  Time
  (->data [x]
    [(.-id x) (.-t x)])

  TimeSpan
  (->data [x]
    [(.-t0 x)
     (.-t1 x)])
  (to-t [& [x1 x2 x3]]
    (TimeSpan. x1 x2))

  PState (->data [x]
           [(map ->data (.-acc x))
            (.-id-times x)
            (.-id-stats x)])

  PStats
  (->data [x] [(->data (.-pd x))
               (.-t1 x)
               (map ->data (.-tspans x))
               @x])

  PData
  (->data [x] [(.-nmax x)
               (.-t0 x)
               (->data (.-pstate_ x))]))

(defn data->pstats [->data]
  (let [[pdata t1 tspans realized] ->data
        [nmax t0 pstate] pdata

        ;; pdata (PData. nmax t0 pstate)
        tspans' (map (fn [[t0 t1]] (TimeSpan. t0 t1)) tspans)

        [acc id-times id-stats] pstate

        pstate' (PState. (map (fn [[id t0]] (Time. id t0)) acc) id-times id-stats)

        pdata' (PData. nmax t0 pstate')]
    (PStats. pdata' t1 tspans' (delay realized))))

(nippy/extend-thaw :tufte/pStats
                   [s]
                   (let [read (clojure.edn/read-string (.readUTF s))]
                     (data->pstats read)))

(nippy/extend-freeze PStats :tufte/pStats
                     [x data-output]
                     (.writeUTF data-output
                                (str (->data x))))


(def all-requests {})


(defn merge-grouped-pstats [g1 g2]
  )


(defn hydrate-pstats [pstats]
  (reduce-kv
   (fn [m k v]
     (assoc m k (data->pstats v)))
   {}
   pstats))

;; (tufte/format-grouped-pstats (merge-with tufte/merge-pstats h1 h2))

(defonce reports (atom []))


;; (tufte/merge-pstats (first @reports) (second @reports))
;; (tufte/format-grouped-pstats (into {} (merge-with tufte/merge-pstats @reports)))

;; (reduce
;;  (fn [acc itm]
;;    (tufte/merge-pstats acc itm))
;;  (first @reports)
;;  (rest @reports))
;; ;; (hydrate-pstats tz)

;; (def tz (clojure.edn/read-string (:body (client/get "http://localhost/perf-report"))))

(defn perf-report [req]
  (let [res (client/get "http://localhost/perf-report")]
    (response/response
     (with-out-str
       (clojure.pprint/pprint {:body (:body res)})))))

(defn req [req]
  (let [res (client/get "http://localhost/perf-report")
        grouped-pstats (hydrate-pstats (clojure.edn/read-string (:body res)))]

    (swap! reports conj grouped-pstats)

    (response/response
     (with-out-str
       (clojure.pprint/pprint
        (tufte/format-grouped-pstats
         grouped-pstats))))))

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

(defn merge-k [k]
  (reduce
   (fn [acc itm]
     (if (nil? acc)
       itm
       (tufte/merge-pstats acc itm)))
   nil
   (map k @reports)))

(defn index [request]
  (response/response
   (html
    [:pre
     (str request)]
    [:h1 "Index"]
    [:div#app
     [:h1 "App element"]]
    [:pre
     (with-out-str (clojure.pprint/pprint (count @reports)))
     #_(tufte/format-grouped-pstats
      (into {} (merge-with tufte/merge-pstats @reports)))]
    (if (= :dev (:build env))
      (include-js "/assets/dev-main.js")
      (include-js "/assets/main.js")))))

;; (tufte/format-grouped-pstats (into {} (merge-with tufte/merge-pstats @reports)))

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
