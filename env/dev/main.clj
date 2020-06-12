(ns main
  (:require
   [tuftedash.core :as core]
   [nrepl.server :as nrepl-server]
   [cider.nrepl :refer (cider-nrepl-handler)])
  (:gen-class))

(defn -main [& args]
  (nrepl-server/start-server :port 6523 :handler cider-nrepl-handler)
  (core/-main args))
