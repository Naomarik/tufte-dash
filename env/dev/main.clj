(ns main
  (:require
   ;; [refactor-nrepl.middleware]
   [tuftedash.core :as core]
   ;; [nrepl.server :as nrepl-server]
   #_[cider.nrepl :refer (cider-nrepl-handler)])
  (:gen-class))

(defn -main [& args]
  ;; (spit ".nrepl-port" 39000)
  ;; (nrepl-server/start-server :port 39000
  ;;                            :handler (apply nrepl-server/default-handler
  ;;                                            (map resolve (concat
  ;;                                                          ['refactor-nrepl.middleware/wrap-refactor]
  ;;                                                          cider.nrepl/cider-middleware))))
  (core/-main args))


;; (require 'shadow.cljs.devtools.server) (shadow.cljs.devtools.server/start!)
;; (do (require '[shadow.cljs.devtools.api :as shadow]) (shadow/watch :dev) (shadow/nrepl-select :dev))


;; (require 'figwheel.main.api)
;; (figwheel.main.api/start :dev)
