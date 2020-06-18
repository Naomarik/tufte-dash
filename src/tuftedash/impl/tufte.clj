(ns tuftedash.impl.tufte
  (:require [clojure.tools.reader.edn :as edn]
            [taoensso.nippy :as nippy]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.tufte :as tufte]
            [taoensso.encore :as encore])
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream
    DataOutput 
    DataOutputStream]
   [taoensso.tufte.impl PData PState PStats Time TimeSpan]))

(set! *warn-on-reflection* true)
(def _graalvm_compile-this_ (net.jpountz.lz4.LZ4Factory/fastestInstance))

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
               (map ->data (.-tspans x)) ; may be able to nil this for smaller file size
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

(defn write-chunked [^DataOutputStream os data]
  (let [chunks (doall (map str/join (partition-all 50000 (str data))))]
    (.writeInt os (count chunks))
    (println "CHunks" (count chunks))
    (doall (map
            (fn [^String s] (.writeUTF os s))
            chunks))
    (.writeUTF os "\n") ;; without this nippy fails
    ))

(defn read-chunked [^DataInputStream is]
  (loop [s ""
         n (.readInt is)]
    (let [new-s (.readUTF is)]
      (if (> n 0)
        (recur (str s new-s)
               (dec n))
        s))))

(comment
  (do
   (def baos (java.io.ByteArrayOutputStream. 64))
   (def os (java.io.DataOutputStream. baos))
   (write-chunked os {:test true
                      :long (str/join (take 300 (repeat "f")))
                      })

   (.flush os)
   (def is (java.io.DataInputStream. (java.io.ByteArrayInputStream. (.toByteArray baos))))
   (.readInt is)
   (read-chunked is)))

(nippy/extend-freeze PStats :tufte/pStats
                     [data os]
                     (write-chunked os (->data data)))

(nippy/extend-thaw :tufte/pStats
                   [is]
                   (let [read (edn/read-string (read-chunked is))]
                     (data->pstats read)))

(defn hydrate-pstats [pstats]
  (reduce-kv
   (fn [m k v]
     (assoc m k (data->pstats v)))
   {}
   pstats))

(defn load-data [file-name]
  (let [exists? (.exists (io/file file-name))
        ret (if exists?
              (nippy/thaw-from-file file-name)
              {})]
    ret))
