(ns snake.views
  (:require
    [hiccup
      [page :refer [html5]]
      [element :refer [javascript-tag]]
      [page :refer [include-js]]]))

(defn- run-clojurescript [path init]
  (list
    (include-js path)
    (javascript-tag init)))

(defn index-page []
  (html5
    [:head
      [:title "Snake for ClojureScript"] ]
    [:body
      [:h1 "Snake for ClojureScript"]
     [:canvas {:id "snake" :width="200" :height "200"}
      "Your browser does not support HTML5 canvas elements."]
      (run-clojurescript
        "/js/main-debug.js"
        "snake.repl.connect()")]))
