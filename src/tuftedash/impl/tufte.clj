(ns tuftedash.impl.tufte
  (:require
   [taoensso.encore :as encore]
   [env :refer [env]]
   [taoensso.nippy :as nippy]
   [taoensso.tufte :as tufte]
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [taoensso.tufte.impl PData PStats PState TimeSpan Time]))

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
                   (let [read (edn/read-string (.readUTF s))]
                     (data->pstats read)))

(nippy/extend-freeze PStats :tufte/pStats
                     [x data-output]
                     (.writeUTF data-output
                                (str (->data x))))

(defn hydrate-pstats [pstats]
  (reduce-kv
   (fn [m k v]
     (assoc m k (data->pstats v)))
   {}
   pstats))
