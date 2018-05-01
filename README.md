# Self-improving tic tac toe implementation

This tic tac toe implementation is supposed to learn from its mistakes! State is maintained as two integers, one for each player. Each bit signifies a checked/unchecked square.

The game does not try to evaluate moves, it picks random moves that are valid (unchecked squares) and not blacklisted.The game learns by blacklisting moves that lead to defeat. If all moves in a certain position lead to defeat, the move into that position is blacklisted. Winning moves are remembered and used rather than random moves.

## Development mode

To start the Figwheel compiler, navigate to the project folder and run the following command in the terminal:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser. The server will be available at [http://localhost:3449](http://localhost:3449) once Figwheel starts up.

Figwheel also starts `nREPL` using the value of the `:nrepl-port` in the `:figwheel`
config found in `project.clj`. By default the port is set to `7002`.

The figwheel server can have unexpected behaviors in some situations such as when using
websockets. In this case it's recommended to run a standalone instance of a web server as follows:

```
lein do clean, run
```

The application will now be available at [http://localhost:3000](http://localhost:3000).


### Optional development tools

Start the browser REPL:

```
$ lein repl
```
The Jetty server can be started by running:

```clojure
(start-server)
```
and stopped by running:
```clojure
(stop-server)
```


## Building for release

```
lein do clean, uberjar
```
