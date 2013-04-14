(ns snake.routes
  (:use compojure.core
        snake.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
  (GET "/" [] (index-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))

; run these three to start a server on the repl
#_(use 'ring.adapter.jetty)
#_(def server (run-jetty #'app {:port 3333 :join? false}))

; control the server
#_(.stop server)
#_(.start server)
