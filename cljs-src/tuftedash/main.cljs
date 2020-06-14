(ns ^:figwheel-hooks tuftedash.main
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rd]
   [reagent.core :as reagent :refer [atom]]))

(defn init []
  (js/console.log "main"))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello word!"}))

(defn get-app-element []
  (gdom/getElement "app"))

(defn hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "Hello"]])

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
