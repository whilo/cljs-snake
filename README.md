# cljs-snake

This port uses HTML5 canvas and ClojureScript instead of
the original version in Swing and Clojure. Adjust
panel-canvas-id near the top to id of your canvas element.

Original code from:
from: http://java.ociweb.com/mark/programming/ClojureSnake.html

"This is a Swing-based game where the arrow keys to guide
a snake to apples.  Each time the snake eats an apple it
grows and a new apple appears in a random location.
If the head of the snake hits its body, you lose.
If the snake grows to a length of 10, you win.
In either case the game starts over with a new, baby snake.

This was originally written by Abhishek Reddy.  Mark Volkmann rewrote it
in an attempt to make it easier to understand.  Christian Weilbach
ported it to ClojureScript with some fixes."

# usage

Use 'lein cljsbuild once' to build with lein-cljsbuild and use 'lein
run' to start the server or start two REPLs, in one start the
jetty-server in routes.clj in the other the piggyback REPL from
core.cljs.

The site is deployed on port 3333.
