(ns user
  (:require
   [tuftedash.core :as core]))

(defn -main [& args]
  (println "LOADDED")
  ;; (spit ".nrepl-port" 39000)
  #_(nrepl-server/start-server :port 39000
                               :handler (apply nrepl-server/default-handler
                                               (map resolve (concat
                                                             ['refactor-nrepl.middleware/wrap-refactor]
                                                             cider.nrepl/cider-middleware))))
  (Thread. (core/-main args)))

(future (-main))
