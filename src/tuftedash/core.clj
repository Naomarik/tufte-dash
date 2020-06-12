(ns tuftedash.core
  (:require
   [tuftedash.app :as app]
   [mount.core :refer [defstate] :as mount]
   [env :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 3080
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc] ; Prior to 0.4.1, you would have to use:
   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defstate http-server
  :start
  (let [port (:port env)]
    (println "Started on port " port)
    (run-jetty app/handler {:port port}))
  :stop
  (.stop http-server))

(defn -main [& args]
  (let [parsed (parse-opts args cli-options)
        {{:keys [port]} :options} parsed]

    (mount/start)))
