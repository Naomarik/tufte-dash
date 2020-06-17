(ns ^:figwheel-hooks tuftedash.main
  (:require
   [cljs.tools.reader.edn :as edn]
   [tuftedash.utils :as utils]
   [goog.dom :as gdom]
   [ajax.core :refer [GET POST]]
   [reagent.dom :as rd]
   [reagent.core :as r]))

(defn init []
  (js/console.log "main"))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (r/atom {:text "Hello word!"}))

(defn get-app-element []
  (gdom/getElement "app"))

(defn fetch-meta []
  (GET "/report-meta"
       {:handler (fn [response]
                   (swap! app-state assoc :meta (edn/read-string response)))}))

(defn fetch-report []
  (let [{:keys [current-tag start end]} @app-state
        [_ [start-uuid _]] start
        [_ [end-uuid _]] end]
    (into {}
          (filter second {:start-uuid start-uuid
                          :end-uuid end-uuid
                          :tag (str current-tag)}))
    (GET "/report"
         {:params (into {}
                        (filter second {:start-uuid start-uuid
                                        :end-uuid end-uuid
                                        :tag (str current-tag)}))
          :handler (fn [response]
                     (swap! app-state assoc :data response))})))


(defn tags []
  (fn []
    (let [selected (:current-tag @app-state)]
      [:div
       [:h3 "Tags"]
       (into [:ul]
             (map
              (fn [t]
                [:li
                 {:on-click
                  (fn []
                    (swap! app-state assoc :current-tag t)
                    (fetch-report))
                  :class (when (= selected t) "active")
                  }
                 (str t)])
              (map :tag (get-in @app-state [:meta :tags]))))])))

(defn tag-ts []
  (let [[start end] ((juxt :start :end) @app-state)]
    [:div#range
     [:h3 "Range"]
     (let [[start-idx end-idx] (map first [start end])
           selected-tag (get-in @app-state [:current-tag])
           tag-meta (get-in @app-state [:meta :tags])
           sel-range (:range (first (filter #(= (:tag %) selected-tag) tag-meta)))]
       #_(when-let [[idx [uuid ts]] start]
           [:div#start
            "Start "
            [:span.date
             (utils/udt->format ts "hh:mm A,  d/MMM/yy")]])
       (into [:ul]
             (map-indexed
              (fn [idx [uuid ts]]
                [:li
                 {:on-click
                  (fn [] nil)}
                 [:span (inc idx)]
                 [:span.date
                  (utils/udt->format ts "hh:mm A,  d/MMM/yy")]
                 [:button
                  {:on-click #(do
                                (if (= idx start-idx)
                                  (swap! app-state dissoc :start)
                                  (do
                                    (when (> idx end-idx)
                                      (swap! app-state dissoc :end))
                                    (swap! app-state assoc :start [idx [uuid ts]]))))
                   :class (when (= idx start-idx) "active")}
                  "Start"]
                 (when
                     (and start-idx
                          (> idx start-idx))
                   [:button
                    {:on-click #(do
                                  (if (= idx end-idx)
                                    (swap! app-state dissoc :end)
                                    (swap! app-state assoc :end [idx [uuid ts]])))
                     :class (when (= idx end-idx) "active")}
                    "End"])])
              sel-range)))]))

(defn report []
  []
  (let [{:keys [start end current-tag]} @app-state]
    [:div#report
     [:h1 "Report"]
     [:div.title
      [:h4 "Tag " (str current-tag)]
      (when start
        (let [[_ [uuid ts]] start]
          [:span
           "From "
           [:span.bold
            (utils/udt->format ts "hh:mm A,  d MMMM yyyy")]]))
      (when end
        (let [[_ [uuid ts]] end]
          [:span
           " to "
           [:span.bold
            (utils/udt->format ts "hh:mm A,  d MMMM yyyy")]]))]
     [:h5 "Total: "
      (get-in @app-state [:data :total])]
     [:pre
      (edn/read-string (get-in @app-state [:data :report]))]]))

(defn hello-world []
  [:div.container
   [:pre (:meta @app-state)]
   [:div.top
    [:button {:on-click #(do
                          (fetch-meta)
                          (fetch-report))} "Let's Get It"]]
   [:div.flex
    [:div.col
     [tags]
     [tag-ts]]
    [:div.col
     [report]]]])

(defn mount [el]
  (rd/render [hello-world] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
