# eventbusbridge-cljs

An example of using the ClojureScript eventbus.

## Usage

First, compile the ClojureScript client:
  
    lein cljsbuild once
    
Then run the server:

    vertx run bridge-server.clj
   
And connect to http://localhost:8080/

Note that if you want to use `vertx.client.eventbus` in your own
ClojureScript code, you'll need to add:

    :foreign-libs [{:file "js/vertxbus.js"
                    :provides ["vertx.eventbusjs"]}
                   {:file "js/sockjs.js"
                    :provides ["sockjs"]}]
                    
To your `:cljsbuild` `:compiler` configuration until
[CLJS-588](http://dev.clojure.org/jira/browse/CLJS-588) is fixed. See
`project.clj`.

See the implementation for the
[client](src/eventbusbridge_cljs/client.cljs) and the
[`vertx.client.eventbus`](https://github.com/vert-x/mod-lang-clojure/blob/master/api/src/main/clojure/vertx/client/eventbus.cljs).
