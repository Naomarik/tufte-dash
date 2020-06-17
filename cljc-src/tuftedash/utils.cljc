(ns tuftedash.utils
  (:require #?@(:clj [[clj-time.core :as t-core]
                      [clj-time.coerce :as t-coerce]
                      [clj-time.format :as t-format]]
                :cljs
                [[cljs-time.format :as t-format]
                 [cljs-time.coerce :as t-coerce]
                 [cljs-time.core :as t-core]])))


(defn udt->format
  "https://github.com/andrewmcveigh/cljs-time/blob/master/src/cljs_time/format.cljs"
  [udt time-format]
  #?(:clj
     (t-format/unparse (t-format/formatter time-format) (t-coerce/from-long udt)))
  #?(:cljs
     (t-format/unparse (t-format/formatter time-format) (t-coerce/from-long udt))))

(defn timestamp-now
  "Return the number of milliseconds from epoch of now"
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn granular-now-date
  "Returns an inst with the lower at the specific granular level. Specificty can be #{:month :day :hour}"
  [specificity]
  (assert (#{:month :day :hour :min} specificity))
  (let [formatter-str (condp = specificity
                        :month "MM/yy"
                        :day "dd/MM/yy"
                        :hour "dd/MM/yy H"
                        :min "dd/MM/yy H m")
        formatter (t-format/formatter formatter-str)]
    (t-coerce/to-date
     (t-format/parse formatter (udt->format (timestamp-now) formatter)))))
