(ns tuftedash.app
  (:require
   [taoensso.encore :as encore]
   [tuftedash.utils :as utils]
   ;; [env :refer [env]]
   ;; [clj-http.client :as client]
   [clj-http.lite.client :as client]
   [clojure.core.async :as async :refer [>! <! chan put! take! timeout close! go alts! go-loop]]
   [taoensso.nippy :as nippy]
   [taoensso.tufte :as tufte]
   [tuftedash.impl.tufte :as tufte-impl]
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io]
   [mount.core :as mount]))

(defn take-while+
  [pred coll]
  (lazy-seq
   (when-let [[f & r] (seq coll)]
     (if (pred f)
       (cons f (take-while+ pred r))
       [f]))))

(def hydrate-pstats tufte-impl/hydrate-pstats)

(def file-name "reports.nippy")
(defonce reports (atom
                  (let [exists? (.exists (io/file file-name))]
                    (if exists?
                      (nippy/thaw-from-file file-name)
                      {}))))

(defn tags []
  (distinct (map :tag (vals @reports))))

(defn reports-for-tag [tag]
  (filter #(= (:tag (second %)) tag) @reports))

(defn time-range-for-tag
  "Returns [{<uuid> <ts>}]"
  [tag]
  (sort-by second (map
    (juxt first (comp :ts second))
    (reports-for-tag tag))))

(defn start-end-ts-for-tag [tag]
  ((juxt first last) (time-range-for-tag tag)))

(comment
  (count (start-end-ts-for-tag
          (first (tags))))
  (count (time-range-for-tag
          (first (tags)))))

(defn meta []
  {:tags (map
          (fn [t]
            {:tag t
             :range (time-range-for-tag t)})
          (tags))
   :total (count @reports)})

(defn select-pstats [opts]
  (let [{:keys [start-uuid end-uuid tag]} opts

        ret (cond
              (and start-uuid end-uuid tag)
              (map @reports
                   (map first (->> (drop-while #(not=
                                                 start-uuid
                                                 (first %)) (time-range-for-tag {:version "0.8"}))
                                   (take-while+ #(not=
                                                  end-uuid (first %))))))

              (and start-uuid tag)
              (map @reports
                   (map first (->> (drop-while #(not= start-uuid (first %))
                                               (time-range-for-tag {:version "0.8"})))))

              tag
              (reports-for-tag tag)

              :else
              (vals @reports))]
    ret))

(defn render-report [opts]
  (def aher opts)
  (let [selected-pstats (select-pstats opts)]
    {:total (count selected-pstats)
     :report (with-out-str
               (clojure.pprint/pprint
                (tufte/format-grouped-pstats
                 (apply merge-with tufte/merge-pstats (map :grouped-pstats selected-pstats)))))}))

(defn fetch-report []
  (let [res (client/get "http://localhost/perf-report" {:query-params {:pass "omar"}})
        {:keys [data uuid tag]} (edn/read-string (:body res))
        grouped-pstats (hydrate-pstats data)]
    (when-not (get @reports uuid)
      (swap! reports assoc uuid {:tag tag
                                 :ts (utils/timestamp-now)
                                 :grouped-pstats grouped-pstats}))))

(defn async-fetch-loop []
  (println "Starting Async Loop")
  (let [kill-chan (chan)
        interval (encore/ms :secs 5)]
    (go-loop [total 0]
      ;; Every X runs clear the resolver cache
      (let [tch (timeout interval)
            [val chan] (alts! [kill-chan tch])]
        (if (= chan tch)
          (do
            (try
              (fetch-report)
              (nippy/freeze-to-file "reports.nippy" @reports)
              (catch Exception e
                (println "Got error " e)))
            (println "Got it, report count" (count @reports))
            (recur (inc total)))
         (do
            (println "Shutting down loop")
            false))))
    #(go (>! kill-chan true))))

(mount/defstate fetch-loop
  :start (async-fetch-loop)
  :stop (fetch-loop))

(comment
  (mount/stop (var fetch-loop))
  (mount/start (var fetch-loop)))
