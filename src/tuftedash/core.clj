(ns tuftedash.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [env :refer [env]]
            [mount.core :as mount :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [tuftedash.app :refer [*url load-data]]
            [tuftedash.handler :as handler])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 3080
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   #_["-f" "--file FILE" "FILE"
    :id :file
    :default "reports.nippy"]
   ["-u" "--url URL" "URL"
    :id :url
    ;; :default "http://localhost/perf-report"]
    :default "http://naomarik.dlinkddns.com:3001/perf-report"]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc] ; Prior to 0.4.1, you would have to use:
   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defstate http-server
  :start
  (let [{:keys [port build]} env]
    (println "Started on port " port)
    (if (= build :dev)
      (do
        (require '[ring.middleware.reload])
        (run-jetty ((resolve 'ring.middleware.reload/wrap-reload) #'handler/handler) {:port port}))
      (run-jetty handler/handler {:port port})))
  :stop
  (.stop http-server)) ;; doesn't work

(comment
  (mount/stop (var http-server))
  (mount/start (var http-server)))

(defn -main [& args]
  (let [parsed (parse-opts args cli-options)
        {{:keys [port url]} :options} parsed]

    (load-data)

    (when url
      (reset! *url url))
    (mount/start)))
