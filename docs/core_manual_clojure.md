<!--
This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/ or send
a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
-->

[TOC]

# Writing Verticles

As was described in the [main manual](manual.html#verticle), a
verticle is the execution unit of Vert.x.

To recap, Vert.x is a container which executed packages of code called
Verticles, and it ensures that the code in the verticle is never
executed concurrently by more than one thread. You can write your
verticles in any of the languages that Vert.x supports, and Vert.x
supports running many verticle instances concurrently in the same
Vert.x instance.

All the code you write in a Vert.x application runs inside a Verticle
instance.

For simple prototyping and trivial tasks you can write raw verticles
and run them directly on the command line, but in most cases you will
always wrap your verticles inside Vert.x [modules](mods_manual.html).

For now, let's try writing a simple raw verticle.

As an example we'll write a simple TCP echo server. The server just
accepts connections and any data received by it is echoed back on the
connection.

Copy the following into a text editor and save it as `server.clj`

    (ns example.server
      (:require [vertx.core :as core]
                [vertx.net :as net]
                [vertx.stream :as stream]))

    (let [server (net/server)]
      (-> server
          (net/on-connect #(stream/pump % %))
          (net/listen 1234 "localhost"))
          
      (core/on-stop
        (.close server)))

Now, go to the directory where you saved the file and type

    vertx run server.clj

The server will now be running. Connect to it using telnet:

    telnet localhost 1234

And notice how data you send (and hit enter) is echoed back to you.

Congratulations! You've written your first verticle.

In the rest of this manual we'll assume the code snippets are running
inside a verticle.

## Accessing the Vert.x API

The Clojure Vert.x API consists of several namespaces that you will
need to require directly or as part of the namespace declaration in
your verticle main file.

## Verticle clean-up

Servers, clients and event bus handlers will be automatically closed
when the verticles is stopped, however if you need to provide any
custom clean-up code when the verticle is stopped you can register any
number of bodies or functions with the `vertx.core/on-stop` macro and
`vertx.core/on-stop*` function, respectively. Vert.x will invoke all
of the registered bodies/functions when the verticle is stopped.

## Getting Configuration in a Verticle

You can pass configuration to a module or verticle from the command
line using the `-conf` option, for example:

    vertx runmod com.mycompany~my-mod~1.0 -conf myconf.json

or for a raw verticle

    vertx run foo.clj -conf myconf.json

The argument to `-conf` is the name of a text file containing a valid
JSON object.

The configuration is available in the verticle as a Clojure map using
the `vertx.core/config` function. For example:
    
    (require '[vertx.core :as core])
        
    ;; Do something with config
    
    (println "number of wibbles is #{config.wibble_number}" 
             (:wibble-number (core/config)))

The config returned is a Clojure map with keyword keys. You can use
this object to configure the verticle. Allowing verticles to be
configured in a consistent way like this allows configuration to be
easily passed to them irrespective of the language.

## Logging from a Verticle

Each verticle is given its own logger. You can log to it using the
functions in `vertx.logging`:

    (require '[vertx.logging :as log])
    
    (log/info "I am logging something")

`vertx.logging` provides the following macros:

* trace
* debug
* info
* warn
* error
* fatal

Which have the normal meanings you would expect.

The log files by default go in a file called `vertx.log` in the system
temp directory. On my Linux box this is `/tmp`.

For more information on configuring logging, please see the
[main manual](manual.html#logging).

## Accessing environment variables from a Verticle

You can access a map of environment variables from a Verticle with the
`vertx.core/env` function.

## Causing the container to exit

You can call the `vertx.core/exit` function to cause the Vert.x
instance to make a clean shutdown.

# Deploying and Undeploying Verticles Programmatically

You can deploy and undeploy verticles programmatically from inside
another verticle. Any verticles deployed this way will be able to see
resources (classes, scripts, other files) of the main verticle.

## Deploying a simple verticle

To deploy a verticle programmatically call the
`vertx.core/deploy-verticle` function.

To deploy a single instance of a verticle :
        
    (core/deploy-verticle main)

Where `main` is the name of the Verticle (i.e. the name of the script
or FQCN of the class).

See the chapter on ["running Vert.x"](manual.html#running-vertx) in
the main manual for a description of what a main is.

## Deploying Worker Verticles

The `vertx.core/deploy-verticle` function deploys standard (non
worker) verticles. If you want to deploy worker verticles use the
`vertx.core/deploy-worker-verticle` function. This function takes the
same parameters as `vertx.core/deploy-verticle` with the same
meanings.
    
## Deploying a module programmatically

You should use `vertx.core/deploy-module` to deploy a module, for example:

    (core/deploy-module "io.vertx~mod-mailer~2.0.0-beta1" 
                        :config config)

Would deploy an instance of the `io.vertx~mod-mailer~2.0.0-beta1`
module with the specified configuration map. Please see the
[modules manual](mods_manual.html) for more information about modules.

## Passing configuration to a verticle programmatically

Configuration can be passed to a verticle that is deployed
programmatically. Inside the deployed verticle the configuration is
accessed with the `vertx.core/config` function. For example:

    (core/deploy-verticle "my_verticle.clj" 
                          :config {:name "foo" :age 234})

Then, in `my_verticle.clj` you can access the config via
`vertx.core/config` as previously explained.

## Using a Verticle to co-ordinate loading of an application

If you have an application that is composed of multiple verticles that
all need to be started at application start-up, then you can use
another verticle that maintains the application configuration and
starts all the other verticles. You can think of this as your
application starter verticle.

For example, you could create a verticle `app.clj` as follows:

    (ns my.app
      (:require [vertx.core :refer
                [config deploy-verticle deploy-worker-verticle]]))
                
    (let [cfg (config)]
      ;; start the verticles that make up the app
      
      (deploy-verticle "verticle1.clj" 
                       :config (:verticle1Config cfg))
      (deploy-verticle "verticle2.clj" 
                       :config (:verticle2Config cfg) 
                       :instances 5)
      (deploy-verticle "verticle3.clj" 
                       :config (:verticle3Config cfg))
      (deploy-worker-verticle "verticle4.clj" 
                              :config (:verticle4Config cfg))
      (deploy-worker-verticle "verticle5.clj" 
                              :config (:verticle5Config cfg) 
                              :instances 10))

Then set the `app.clj` verticle as the main of your module and then
you can start your entire application by simply running:

    vertx runmod com.mycompany~my-mod~1.0 -conf conf.json

Where conf.json is a config file like:

    // Application config
    {
        verticle1Config: {
            // Config for verticle1
        },
        verticle2Config: {
            // Config for verticle2
        }, 
        verticle3Config: {
            // Config for verticle3
        },
        verticle4Config: {
            // Config for verticle4
        },
        verticle5Config: {
            // Config for verticle5
        }  
    }  

If your application is large and actually composed of multiple modules
rather than verticles you can use the same technique.

## Specifying number of instances

By default, when you deploy a verticle only one instance of the
verticle is deployed. Verticles instances are strictly single threaded
so this means you will use at most one core on your server.

Vert.x scales by deploying many verticle instances concurrently.

If you want more than one instance of a particular verticle or module
to be deployed, you can specify the number of instances as follows:

    (core/deploy-verticle "foo.ChildVerticle" 
                          :instances 10) 

Or
    
    (core/deploy-module "io.vertx~some-mod~1.0" 
                        :instances 10)
  
The above examples would deploy 10 instances.

## Getting Notified when Deployment is complete

The actual verticle deployment is asynchronous and might not complete
until some time after the call to `deploy-verticle` or `deploy-module`
has returned. If you want to be notified when the verticle/module has
completed being deployed, you can pass a function to `deploy-verticle`,
which will be called when it's complete:

    (core/deploy-verticle "my_verticle.clj" 
      :handler (fn [err deploy-id]
                 (when-not err
                   (println "It's been deployed OK!"))))

The first parameter passed to the fn is an exception-map which will be not
`nil` if a failure occurred, otherwise it will be `nil`.

The second parameter is the deployment id string. It will be `nil` if
a failure occurred. You will need this if you later want to undeploy
the verticle or module.


## Undeploying a Verticle

Any verticles that you deploy programmatically from within a verticle
and all of their children are automatically undeployed when the parent
verticle is undeployed, so in most cases you will not need to undeploy
a verticle manually, however if you do want to do this, it can be done
by calling the `vertx.core/undeploy-verticle` function, passing in the
deployment id.

    (core/deploy-verticle "my_verticle.rb" 
      :handler (fn [err deploy-id]
                 ;; Immediately undeploy it
                 (when-not err
                   (core/undeploy-verticle deploy-id))))



A verticle instance is almost always single threaded (the only
exception is multi-threaded worker verticles which are an advanced
feature not intended for normal development), this means a single
instance can at most utilise one core of your server.

In order to scale across cores you need to deploy more verticle
instances. The exact numbers depend on your application - how many
verticles there are and of what type.

You can deploy more verticle instances programmatically or on the
command line when deploying your module using the `-instances` command
line option.
            

# The Event Bus

The event bus is the nervous system of Vert.x.

It allows verticles to communicate with each other irrespective of
what language they are written in, and whether they're in the same
Vert.x instance, or in a different Vert.x instance.

It even allows client side JavaScript running in a browser to
communicate on the same event bus. (More on that later).

The event bus forms a distributed polyglot overlay network spanning
multiple server nodes and multiple browsers.

The event bus API is incredibly simple. It basically involves
registering handlers, unregistering handlers and sending and
publishing messages.

First some theory:

## The Theory

### Addressing

Messages are sent on the event bus to an *address*.

Vert.x doesn't bother with any fancy addressing schemes. In Vert.x an
address is simply a string, any string is valid. However it is wise to
use some kind of scheme, e.g. using periods to demarcate a namespace.

Some examples of valid addresses are `europe.news.feed1`,
`acme.games.pacman`, `sausages`, and `X`.

### Handlers

A handler is a thing that receives messages from the bus. You register
a handler at an address.

Many different handlers from the same or different verticles can be
registered at the same address. A single handler can be registered by
the verticle at many different addresses.

### Publish / subscribe messaging

The event bus supports *publishing* messages. Messages are published
to an address. Publishing means delivering the message to all handlers
that are registered at that address. This is the familiar
*publish/subscribe* messaging pattern.

### Point to point and Request-Response messaging

The event bus supports *point to point* messaging. Messages are sent
to an address. Vert.x will then route it to just one of the handlers
registered at that address. If there is more than one handler
registered at the address, one will be chosen using a non-strict
round-robin algorithm.

With point to point messaging, an optional reply handler can be
specified when sending the message. When a message is received by a
recipient, and has been handled, the recipient can optionally decide
to reply to the message. If they do so that reply handler will be
called.

When the reply is received back at the sender, it too can be replied
to. This can be repeated ad-infinitum, and allows a dialog to be
set-up between two different verticles. This is a common messaging
pattern called the *Request-Response* pattern.

### Transient

*All messages in the event bus are transient, and in case of failure
 of all or parts of the event bus, there is a possibility messages
 will be lost. If your application cares about lost messages, you
 should code your handlers to be idempotent, and your senders to retry
 after recovery.*

If you want to persist your messages you can use a persistent work
queue module for that.

### Types of messages

Messages that you send on the event bus can be as simple as a string,
a number or a boolean. You can also send vert.x buffers or JSON.

It's highly recommended you use JSON messages to communicate between
verticles. JSON is easy to create and parse in all the languages that
vert.x supports.

## Event Bus API

Let's jump into the API

### Registering and Unregistering Handlers

To set a message handler on the address `test.address`, you call the
`vertx.eventbus/on-message` function:

    (require '[vertx.eventbus :as eb])

    (eb/on-message "test.address"
      (fn [message]
        (println "Got message body" (eb/body message))))

It's as simple as that. The handler will then receive any messages
sent to that address. The object passed into the handler is an
instance of class `Message`. The body of the message is available via
the `vertx.eventbus/body` function.

The return value of `on-message` is a unique handler id which
can used later to unregister the handler.

When you register a handler on an address and you're in a cluster it
can take some time for the knowledge of that new handler to be
propagated across the entire cluster. If you want to be notified when
that has completed you can optionally specify a function to the
`on-message` function as the third argument. This function will then
be called once the information has reached all nodes of the
cluster. E.g. :

    (eb/on-message "test.address" my-handler 
      (fn [err]
        (println "Yippee! The handler info has been propagated across the cluster")))
    
To unregister a handler it's just as straightforward. You simply call
`unregister-handler` passing in the id of the handler:

    (eb/unregister-handler "test.address" id)

As with registering, when you unregister a handler and you're in a
cluster it can also take some time for the knowledge of that
unregistration to be propagated across the entire to cluster. If you
want to be notified when that has completed you can optionally specify
another function to the `unregister-handler` method:

    (eb/unregister-handler "test.address" id
      (fn [err]
        (println "Yippee! The handler unregister has been propagated across the cluster")))

If you want your handler to live for the full lifetime of your
verticle there is no need to unregister it explicitly - vert.x will
automatically unregister any handlers when the verticle is stopped.

### Publishing messages

Publishing a message is also trivially easy. Just publish it
specifying the address, for example:

    (eb/publish "test.address" "hello world")

That message will then be delivered to all handlers registered against
the address "test.address".

### Sending messages

Sending a message will result in only one handler registered at the
address receiving the message. This is the point to point messaging
pattern. The handler is chosen in a non strict round-robin fashion.

    (eb/send "test.address" "hello world")

### Replying to messages

Sometimes after you send a message you want to receive a reply from
the recipient. This is known as the *request-response pattern*.

To do this you send a message, and specify a function as a reply
handler. When the receiver receives the message, it can use the 
`vertx.eventbus/reply` function to send a reply.

When this function is invoked it causes a reply to be sent back to the
sender where the reply handler is invoked. An example will make this
clear:

The receiver:

    (eb/on-message "test.address"
      (fn [message]
        (println "I received a message" (eb/body message))

        ;; Do some stuff...
        ;; Now reply to it
        
        (eb/reply message "This is a reply")))

The sender:

    (eb/send "test.address" "This is a message"
      (fn [reply-msg]
        (println "I received a reply" (eb/body reply-msg))))

It is legal also to send an empty reply or null reply.

The replies themselves can also be replied to so you can create a
dialog between two different verticles consisting of multiple rounds.

#### Specifying timeouts for replies

If you send a message specifying a reply handler, and the reply never
comes, then, by default, you'll be left with a handler that never gets
unregistered.

To remedy this you can also specify a timeout in ms and a two-arity
reply handler function. If a reply is received before timeout your
handler will be called with the message as the second parameter,
but if no reply is received before timeout, the handler will be
automatically unregistered and your handler will be called with an
exception-map as the first parameter you can deal with it in your code.

Here's an example:

    (eb/send "test.address" "This is a message" 1000 
      (fn [err m]
        (if err
          (println "No reply was received before the 1 second timeout!")
          (println "I received a reply" m))))

If the send times out, first parameter to the handler function will be
an exception map of the form:

    {:type :TIMEOUT
     :message "Timed out waiting for reply"
     :basis the-ReplyException-object}
     

You can also set a default timeout on the event bus itself - this
timeout will be used if you are using the `send` function without a
timeout. The default value of the default timeout is `-1` which means
that reply handlers will never timeout (this is for backward
compatibility reasons with earlier versions of Vert.x). Note that when
using the `send` function without a timeout, your handler will never
be passed an exception-map when a timeout occurs.

    (eb/set-default-reply-timeout! 5000)

When replying to messages you can also provide a timeout and a
two-arity reply handler function to get replies to the replies within a
timeout. The API used is similar to before:

    (eb/on-message "test.address"
      (fn [m]
        (eb/reply "This is a reply" 1000 
          (fn [err m]
            (if err
              (println "No reply was received before the 1 second timeout!")
              (println "I received a reply" m))))))

#### Getting notified of reply failures

If you send a message with a timeout and result handler function, and
there are no handlers available to send the message to, the handler
function will be called with an exception-map containing `:type
:NO_HANDLERS`.

If you send a message with a timeout and result handler, and the
recipent of the message responds by calling `vertx.eventbus/fail`, the
handler function will be called with an exception map of the form:

    {:type :RECIPIENT_FAILURE
     :message "an application specific error message"
     :code an-application-specific-int-code
     :basis the-ReplyException-object}

For example:

    (eb/on-message "test.address"
      (fn [m]
        (eb/fail 123 "Not enough aardvarks")))
        
    
    (eb/send "test.address" "This is a message" 1000 
      (fn [err m]
        (if err
          (do
            (println "Failure type:" (:type err))
            (println "Failure code:" (:code err))
            (println "Failure message: (:message err)))
          (println "I received a reply" m))))
          
### Message types

The message you send can be any of the following types:

* Integer
* Long
* Short
* clojure.lang.BigInt - will be converted to a Long
* Float
* Double
* java.math.BigDecimal - will be converted to a double
* clojure.lang.Ratio - will be converted to a double
* String
* Boolean
* clojure.lang.Seqable (includes clojure lists, vectors, sets) - converted to a JSON array 
* map - converted to a JSON map
* byte[]
* Vert.x Buffer

Vert.x buffers are copied before delivery if they are delivered in the
same JVM, so different verticles can't access the exact same object
instance.

Here are some more examples:

Send some numbers:
    (eb/send "test.address" 1234)
    (eb/send "test.address" 22/7)
    (eb/send "test.address" 1N)

Send a boolean:

    (eb/send "test.address" true)

Send a map (will be converted to a JSON object):

    (eb/send "test.address" {:name "Tim"
                             :address "The Moon"
                             :age 457})

Send a vector (will be converted to a JSON array):

    (eb/send "test.address" ["a" :b 0xC])
    
`nil` messages can also be sent:

    (eb/send "test.address" nil)

It's a good convention to have your verticles communicating using
JSON - this is because JSON is easy to generate and parse for all the
languages that Vert.x supports.

## Distributed event bus

To make each Vert.x instance on your network participate on the same
event bus, start each Vert.x instance with the `-cluster` command line
switch.

See the chapter in the main manual on
[*running Vert.x*](manual.html#running-vertx) for more information on
this.

Once you've done that, any Vert.x instances started in cluster mode
will merge to form a distributed event bus.

# Shared Data

Sometimes it makes sense to allow different verticles instances to
share data in a safe way. Vert.x allows simple *Map* and *Set* data
structures to be shared between verticles. 

There is a caveat: Vert.x ensures that objects are copied where
appropriate on insertion to prevent other verticles having access to
the same instance which could lead to race conditions.

Currently data can only be shared between verticles in the *same
vert.x instance*. In later versions of vert.x we aim to extend this to
allow data to be shared by all vert.x instances in the cluster. 

All Clojure verticles deployed in a single vert.x instance share the
same Clojure runtime, so you can use the Clojure's existing mechanisms
for concurrent coordination. If you have a mix of verticles from other
languages in the same vert.x instance and need to share data, you will
need to use the vert.x shared data mechanisms.

## Shared Maps

To use a shared map to share data between verticles first get a
reference to the map, and then we just use map operations to put and
get the data:

    (require '[vertx.shareddata :as sd])
    
    (-> (sd/get-map "demo.mymap")
        (sd/put! "some-key" "some-value"))

And then, in a different verticle:

    (println "value of some-key is"
      (-> (sd/get-map "demo.mymap")
          (get "some-key")))

## Shared Sets

To use a shared set to share data between verticles first get a
reference to the set:

    (-> (sd/get-set "demo.myset")
        (sd/add! "some-value"))

And then, in a different verticle:
    
    (println "does the set contain some-value?"
      (-> (sd/get-set "demo.myset")
          (contains? "some-key")))

The `vertx.shareddata` namespace provides a few functions for mutating
its maps and sets (`add!`, `put!`, `remove!`, and `clear!`). For
retrieving values from the maps/sets, you can use the built-in clojure
functions (`get`, `contains?`, `count`, `empty?`, etc.).

As a convenience, `add!`, `put!`, `remove!`, and `clear!` can be given
a string or keyword name for a set or map instead of the collection
object and will look it up.

Currently, no conversion is performed on keys
or values placed in shared data. The onus is on you to make sure you
are storing a type that will be readable in any other verticles that
access it.

# Buffers

Most data in vert.x is shuffled around using instances of
`org.vertx.java.core.buffer.Buffer`, which are provided and
manipulated by functions in the `vertx.buffer` namespace.

A Buffer represents a sequence of zero or more bytes that can be
written to or read from, and which expands automatically as necessary
to accomodate any bytes written to it.

## Creating Buffers

Create an empty buffer:
    
    (require '[vertx.buffer :as buf])

    (buf/buffer)

Create a buffer from a String. The String will be encoded in the
buffer using UTF-8:

    (buf/buffer "some-string")
    
Create a buffer from a String. The String will be encoded using the
specified encoding, e.g:

    (buf/buffer "some-string" "UTF-16")
    
Create a buffer with an initial size hint. If you know your buffer
will have a certain amount of data written to it you can create the
buffer and specify this size. This makes the buffer initially allocate
that much memory and is more efficient than the buffer automatically
resizing multiple times as data is written to it:

    (buf/buffer 100000)
    
Note that buffers created this way *are empty*. It does not create a
buffer filled with zeros up to the specified size.
        
Also note that buffers are *mutable*. To emphasize that, we recommend
naming any vars that point to buffers with a trailing `!`. Example:

    (let [some-buf! (buf/buffer)
      ;;mutate away!
      )
      
## Writing to a Buffer

There are two ways to write to a buffer: appending, and random
access. In either case buffers will always expand automatically to
encompass the bytes. It's not possible to write outside the bounds of
the buffer.

### Appending to a Buffer

To append to a buffer, you use `vertx.buffer/append!`. `append!` can
append other buffers, Bytes, byte[]'s, Doubles, BigDecimals (as
Doubles), Ratios (as Doubles), Floats, Integers, Longs, BigInts (as
Longs), Shorts, and Strings.

The return value of `append!` is the buffer itself, so calls can be
chained:

    (-> (buf/buffer)
        (buf/append! 100)
        (buf/append! "hi") ;; as UTF-8
        (buf/append! "hello" "UTF-16")
        (buf/append! some-other-buffer))
    
### Random access buffer writes

You can also write into the buffer at a specific index, by using
`vertx.buffer/set!`. `set!` can handle the same types as `append!`.
`set!` takes an index as the first argument - this represents the
position in the buffer where to start writing the data.

The buffer will always expand as necessary to accomodate the data.

The return value of `append!` is the buffer itself, so calls can be
chained:

    (-> (buf/buffer)
        (buf/set! 0 100)
        (buf/set! 23 "hi") ;; as UTF-8
        (buf/set! 99 "hello" "UTF-16")
        (buf/set! 2000 some-other-buffer))
           
## Reading from a Buffer

Data is read from a buffer using the `vertx.buffer/get-XXX`
functions. Get functions exist for Buffer, byte, int, long, double,
float, short, byte[], and String. The first argument to these methods
is an index in the buffer from where to get the data.

    (buf/get-byte a-buf 100)             ;; Get a byte from pos 100 in buffer

    (buf/get-float a-buf 100)            ;; Get a float from pos 100

    (buf/get-string a-buf 100)           ;; Get a float from pos 100 as UTF-8

    (buf/get-string a-buf 100 "UTF-16")  ;; Get a float from pos 100 as UTF-8

    (buf/get-buffer a-buf 100 110)       ;; Get 10 bytes as a new buffer starting at position 100
    
    
# Delayed and Periodic Tasks

It's very common in vert.x to want to perform an action after a delay,
or periodically.

In standard verticles you can't just make the thread sleep to
introduce a delay, as that will block the event loop thread.

Instead you use vert.x timers. Timers can be *one-shot* or
*periodic*. We'll discuss both.

## One-shot Timers

A one shot timer calls an event handler after a certain delay,
expressed in milliseconds.

To set a timer to fire once you use the `vertx.core/timer` macro,
passing in the delay and specifying a handler body which will be
called after the delay:

    (require '[vertx.core :as core])
    
    (core/timer 1000
      (println "And one second later this is printed"))

    (println "First this is printed")

The return value of the function is a unique timer id. You can use this
to subsequently cancel the timer. You can also use the The
`vertx.core/timer*` function to have access to the timer id at
execution time:

    (core/timer* 1000
      #(println "The timer id is" %))


## Periodic Timers

You can also set a timer to fire periodically by using the
`vertx.core/periodic` macro, passing in the delay and specifying a
handler body which will be called every delay ms:

    (core/periodic 1000
      (println "And every second this is printed"))

    (println "First this is printed")

The return value of the function is a unique timer id. You can use this
to subsequently cancel the timer. You can also use the The
`vertx.core/periodic*` function to have access to the timer id at
execution time:

    (core/periodic* 1000
      #(println "The timer id is" %))
      
## Cancelling timers

To cancel a timer, call the `vertx.core/cancel-timer` function
specifying the timer id. For example:

    (let [timer-id (core/periodic 1000
                     ;; This will never be called
                     )]
     
      ;; And immediately cancel it
      
      (core/cancel-timer timer-id)
     
Or you can cancel it from inside the event handler. The following
example cancels the timer after it has fired 10 times:

    (let [count (atom 0)]
      (core/periodic* 1000
        (fn [id]
          (println "In event handler" @count)
          (if (= 10 (swap! count inc))
            (core/cancel-timer id)))))


# Writing TCP Servers and Clients

Creating TCP servers and clients is very easy with vert.x.

## Net Server

### Creating a Net Server

To create a TCP server we simply call `vertx.net/server`:

    (require '[vertx.net :as net])
    
    (net/server)
    
### Start the Server Listening

To tell that server to listen for connections we do:

    (-> (net/server)
        (listen 1234 "myhost"))

The second parameter to `listen` is the port. A wildcard port of `0`
can be specified which means a random available port will be chosen to
actually listen at. Once the server has completed listening you can
then call the `.port` method of the server to find out the real port
it is using.

The third parameter is the hostname or ip address. If it is omitted
it will default to "0.0.0.0" which means it will listen at all
available interfaces.

The actual bind is asynchronous so the server might not actually be
listening until some time *after* the call to listen has returned. If
you want to be notified when the server is actually listening you can
provide a handler function to the `listen` call. For example:

    (net/listen server 1234 "myhost" 
      (fn [err server]
	    (when-not err 
          (println "Now listening!"))))

### Getting Notified of Incoming Connections

To be notified when a connection occurs we need to call the
`vertx.net/on-connect` function, specifying a function which
represents the handler. The handler will be called when a connection
is made, getting the NetSocket. `on-connect` returns the server,
allowing invocations to be threaded:

    (-> (net/socket)
        (net/on-connect 
          (fn [socket]
            (println "A client has connected!")))
        (net/listen 1234))

That's a bit more interesting. Now it displays 'A client has
connected!' every time a client connects.

### Closing a Net Server

To close a net server just call the `close` function:

    (net/close server)

The close is actually asynchronous and might not complete until some
time after the `close` function has returned. If you want to be notified
when the actual close has completed then you can specify a handler function to
the `close` function.

This function will then be called when the close has fully completed.

    (net/close server (fn [err]
                        (when-not err
                          (println "The server is now fully closed."))))
    
    
In most cases you don't need to close a net server explicitly since
vert.x will close them for you when the verticle stops.


### Net Server Properties

Net servers have a set of properties you can set which affect its
behaviour. Firstly there are bunch of properties used to tweak the TCP
parameters, in most cases you won't need to set these:

* `:tcp-no-delay` If this property is true then
  [Nagle's Algorithm](http://en.wikipedia.org/wiki/Nagle's_algorithm)
  is disabled. If false then it is enabled.

* `:send-buffer-size` Sets the TCP send buffer size in bytes.

* `:receive-buffer-size` Sets the TCP receive buffer size in bytes.

* `:tcp-keep-alive` if this property is true then
  [TCP keep alive](http://en.wikipedia.org/wiki/Keepalive#TCP-keepalive)
  is enabled, if false it is disabled.

* `:reuse-address` if this property is true then addresses in
  TIME-WAIT state can be reused after they have been closed.

* `:so-linger`

* `:traffic-class`

You can set these properties at server creation time by passing them
as a map to `vertx.net/server`:

    (net/server {:tcp-no-delay false})
    
Or set them on an existing server object, either by passing them as a
map to `vertx.utils/set-properties`:

    (utils/set-properties server {:tcp-no-delay false})

Or by calling the corresponding java setter methods on the server
object:

    (.setTcpNoDelay server false)
    
Net servers have a further set of properties which are used to
configure SSL. We'll discuss those later on.


### Handling Data

So far we have seen how to create a `NetServer`, and accept incoming
connections, but not how to do anything interesting with the
connections. Let's remedy that now.

When a connection is made, the connect handler is called passing in an
instance of `NetSocket`. This is a socket-like interface to the actual
connection, and allows you to read and write data as well as do
various other things like close the socket.


#### Reading Data from the Socket

To read data from the socket you need to set the data handler on the
socket via `vertx.stream/on-data`. This handler will be called with a
`Buffer` every time data is received on the socket. You could try the
following code and telnet to it to send some data:

    (require '[vertx.net :as net]
             '[vertx.stream :as stream])
             
    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                (println "I received" (.length buffer) "bytes of data")))))
        (net/listen 1234 "localhost"))
        
#### Writing Data to a Socket

To write data to a socket, you invoke the `vertx.stream/write` function. 

With a single buffer:

    (stream/write sock some-buffer)

A string. In this case the string will encoded using UTF-8 and the
result written to the wire:

    (stream/write sock "hello")

A string and an encoding. In this case the string will encoded using
the specified encoding and the result written to the wire:

    (stream/write sock "hello" "UTF-16")

The write function in asynchronous and always returns immediately
after the write has been queued. The actual write might occur some
time later.

Let's put it all together.

Here's an example of a simple TCP echo server which simply writes back
(echoes) everything that it receives on the socket:

    (require '[vertx.net :as net]
             '[vertx.stream :as stream])
             
    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                (stream/write sock buffer)))))
        (net/listen 1234 "localhost"))

### Closing a socket

You can close a socket by invoking its `close` java method. This will close
the underlying TCP connection.

### Closed Handler

If you want to be notified when a socket is closed, you can set the a
closed handler on the socket via `vertx.net/on-close`:

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (net/on-close sock
              (partial println "The socket is now closed"))))
        (net/listen 1234 "localhost"))
    
The closed handler will be called irrespective of whether the close
was initiated by the client or server.

### Exception handler

You can set an exception handler on the socket that will be called if
an exception occurs (note that we're using a function from the
`vertx.stream` namespace):

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-exception sock
              (fn [ex-map] 
                (println "Oops. Something went wrong" (:message ex-map))))))
    (net/listen 1234 "localhost"))

### Event Bus Write Handler

Every NetSocket automatically registers a handler on the event bus,
and when any buffers are received in this handler, it writes them to
itself. This enables you to write data to a NetSocket which is
potentially in a completely different verticle or even in a different
Vert.x instance by sending the buffer to the address of that handler.

**Note that you must explicitly write a buffer to
  `vertx.eventbus/send` when sending to a socket.**

The address of the handler is provided by the `.writeHandlerID` Java
method.

For example to write some data to the NetSocket from a completely
different verticle you could do:

    ;; retrieve the write handler ID and share it via 
    ;; some mechanism (eventbus, shared data, atom, etc)
    (.writeHandlerID sock)
    
    ;; in another verticle, get the ID and send a buffer
    (eb/send write-handler-id (buf/buffer "some data"))


### Read and Write Streams

NetSocket also can at as a `ReadStream` and a `WriteStream`. This
allows flow control to occur on the connection and the connection data
to be pumped to and from other object such as HTTP requests and
responses, WebSockets and asynchronous files.

This will be discussed in depth in the chapter on
[streams and pumps](#flow-control).

## Scaling TCP Servers

A verticle instance is strictly single threaded.

If you create a simple TCP server and deploy a single instance of it
then all the handlers for that server are always executed on the same
event loop (thread).

This means that if you are running on a server with a lot of cores,
and you only have this one instance deployed then you will have at
most one core utilised on your server!

To remedy this you can simply deploy more instances of the module in
the server, e.g.

    vertx runmod com.mycompany~my-mod~1.0 -instances 20

Or for a raw verticle

    vertx run foo.MyApp -instances 20
    
The above would run 20 instances of the module/verticle in the same
Vert.x instance.

Once you do this you will find the echo server works functionally
identically to before, but, *as if by magic*, all your cores on your
server can be utilised and more work can be handled.

At this point you might be asking yourself *'Hold on, how can you have
more than one server listening on the same host and port? Surely you
will get port conflicts as soon as you try and deploy more than one
instance?'*

*Vert.x does a little magic here*.

When you deploy another server on the same host and port as an
existing server it doesn't actually try and create a new server
listening on the same host/port.

Instead it internally maintains just a single server, and, as incoming
connections arrive it distributes them in a round-robin fashion to any
of the connect handlers set by the verticles.

Consequently Vert.x TCP servers can scale over available cores while
each Vert.x verticle instance remains strictly single threaded, and
you don't have to do any special tricks like writing load-balancers in
order to scale your server on your multi-core machine.

## NetClient

A NetClient is used to make TCP connections to servers.

### Creating a Net Client

To create a TCP client we simply call `vertx.net/client`:

    (net/client)

### Making a Connection

To actually connect to a server you invoke the `connect` function:

    (-> (net/client)
        (net/connect 1234 "localhost"
          (fn [err sock]
            (if-not err
              (println "We have connected")))))

The connect function takes the port number as the first parameter,
followed by the hostname or ip address of the server. It takes a block
as the connect handler. This handler will be called when the
connection actually occurs.

The first argument passed into the connect handler is an exception-map -
this will be `nil` if the connect succeeded. The second argument is
the socket itself - this will be `nil` if the connect failed.

The socket object is an instance of `NetSocket`, exactly the same as
what is passed into the server side connect handler. Once given the
`NetSocket` you can read and write data from the socket in exactly the
same way as you do on the server side.

You can also close it, set the closed handler, set the exception
handler and use it as a `ReadStream` or `WriteStream` exactly the same
as the server side `NetSocket`.

### Configuring Reconnection

A NetClient can be configured to automatically retry connecting or
reconnecting to the server in the event that it cannot connect or has
lost its connection. This is done by setting the following properties:

* `:reconnect-attempts` determines how many times the client will try
  to connect to the server before giving up. A value of `-1`
  represents an infinite number of times. The default value is
  `0`. I.e. no reconnection is attempted.
* `:reconnect-interval` determines how long, in milliseconds, the
  client will wait between reconnect attempts. The default value is
  `1000`.

You can set these properties at client creation time by passing them
as a map to `vertx.net/client`:

    (net/client {:reconnect-attempts -1})
    
Or set them on an existing client object, either by passing them as a
map to `vertx.utils/set-properties`:

    (utils/set-properties client {:reconnect-attempts -1})

Or by calling the corresponding java setter methods on the client
object:

    (.setReconnectAttempts client -1)


Just like `NetServer`, `NetClient` also has a set of TCP properties
you can set which affect its behaviour. They have the same meaning as
those on `NetServer`.

`NetClient` also has a further set of properties which are used to
configure SSL. We'll discuss those later on.

## SSL Servers

Net servers can also be configured to work with
[Transport Layer Security](http://en.wikipedia.org/wiki/Transport_Layer_Security)
(previously known as SSL).

When a `NetServer` is working as an SSL Server the API of the
`NetServer` and `NetSocket` is identical compared to when it is
working with standard sockets. Getting the server to use SSL is just a
matter of configuring the `NetServer` before `listen` is called.

To enabled SSL set the property `:ssl` to `true` on the `NetServer`.

The server must also be configured with a *key store* and an optional
*trust store*.

These are both *Java keystores* which can be managed using the
[keytool](http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)
utility which ships with the JDK.

The keytool command allows you to create keystores, and import and
export certificates from them.

The key store should contain the server certificate. This is
mandatory - the client will not be able to connect to the server over
SSL if the server does not have a certificate.

The key store is configured on the server using the properties
`:key-store-path` and `:key-store-password`.

The trust store is optional and contains the certificates of any
clients it should trust. This is only used if client authentication is
required.

To configure a server to use server certificates only:

    (net/server 
      {:ssl true
       :key-store-path "/path/to/your/keystore/server-keystore.jks"
       :key-store-password "password"})
       
Making sure that `server-keystore.jks` contains the server
certificate.

To configure a server to also require client certificates:

    (net/server 
      {:ssl true
       :key-store-path "/path/to/your/keystore/server-keystore.jks"
       :key-store-password "password"
       :client-auth-required true
       :trust-store-path "/path/to/your/truststore/server-truststore.jks"
       :trust-store-password "password"})
       
Making sure that `server-truststore.jks` contains the certificates of
any clients who the server trusts.

If the property `:client-auth-required` is set to `true` and the
client cannot provide a certificate, or it provides a certificate that
the server does not trust then the connection attempt will not
succeed.

## SSL Clients

Net Clients can also be easily configured to use SSL. They have the
exact same API when using SSL as when using standard sockets.

To enable SSL on a `NetClient` the property `:ssl` must be set to `true`.

If the property `:trust-all` has been set to `true`, then the client
will trust all server certificates. The connection will still be
encrypted but this mode is vulnerable to 'man in the middle'
attacks. I.e. you can't be sure who you are connecting to. Use this
with caution.

If `:trust-all` is set to `false`, then a client trust store must be
configured and should contain the certificates of the servers that the
client trusts.

The default value of `:trust-all` is `false`.

The client trust store is just a standard Java key store, the same as
the key stores on the server side. The client trust store location is
set by setting the property `trust_store_path` on the `NetClient`. If
a server presents a certificate during connection which is not in the
client trust store, the connection attempt will not succeed.

If the server requires client authentication then the client must
present its own certificate to the server when connecting. This
certificate should reside in the client key store. Again it's just a
regular Java key store. The client keystore location is set with the
property `:key-store-path` on the `NetClient`.

To configure a client to trust all server certificates (dangerous):

    (net/client
      {:ssl true
       :trust-all true})
    
To configure a client to only trust those certificates it has in its
trust store:

    (net/client
      {:ssl true
       :trust-store-path "/path/to/your/client/truststore/client-truststore.jks"
       :trust-store-password "password"})

To configure a client to only trust those certificates it has in its
trust store, and also to supply a client certificate:

    (net/client
      {:ssl true
       :trust-store-path "/path/to/your/client/truststore/client-truststore.jks"
       :trust-store-password "password"
       :key-store-path "/path/to/keystore/holding/client/cert/client-keystore.jks"
       :key-store-password "password"})

# User Datagram Protocol (UDP) 

Using User Datagram Protocol (UDP) with Vert.x is a piece of cake. 

UDP is a connection-less transport which basically means you have no
persistent connection to a remote peer.

Instead you can send and receive packages and the remote address is
contained in each of them.

Note that UDP is not as safe as TCP to use, as there are no guarantees
that a sent datagram packet will be received by its endpoint at all.

The only guarantee is that it will either be receive completely or not
at all.

Also you usually can't send data larger than the MTU size of
your network interface, because each datagram will be send as
one packet.

But be aware that even if the packet size is smaller then the MTU it
may still fail.

The size at which it will fail depends on the operating system etc. So
a good rule of thumb is to try to send small packets.

Because of the nature of UDP it is best fit for applications where you
are allowed to drop packets (a monitoring application, for example).

The benefits are that it has a lot less overhead compared to TCP,
which can be handled by the NetServer and NetClient (see above).

## Creating a DatagramSocket

To use send or receive UDP, you first need to create a datagram socket:

    (require '[vertx.datagram :as udp])
    
    (udp/socket)
    
    ;; or specify :ipv4/:ipv6 specifically
    (udp/socket :ipv6)
    
The `socket` function takes additional options - See the 'Datagram
socket properties' section below.

The returned socket is not bound to a specific port. 

## Sending Datagram packets

As mentioned before, User Datagram Protocol (UDP) sends data in
packets to remote peers but is not connected to them in a persistent
fashion.

This means each packet can be sent to a different remote peer.

Sending packets is as easy as shown here:

    (-> (udp/socket)
        (udp/send "content" 1234 "10.0.0.1"))
        
    ;; send with a result handler
    (-> (udp/socket)
        (udp/send "content" 1234 "10.0.0.1"
                  (fn [err socket)
                    (println err))))

Be aware that even if `err` is nil, it only means the data was written
to the network stack, but gives no guarantee that it ever reached or 
will reach the remote peer at all.

If you need such a guarantee then you want to use TCP with some 
handshaking logic build on top.

## Receiving Datagram packets

If you want to receive packets you need to bind the datagram socket by 
calling `vertx.datagram/listen` on it, and register a function to 
receive the packets via `vertx.datagram/on-data`:

    (-> (udp/socket)
        (udp/on-data (fn [packet-map] ...))
        (udp/listen 1234 "0.0.0.0"))

The packet passed to the handler function is a map with the following form:

    {:data a-Buffer
     :basis the-DatagramPacket-object
     :sender {:host "the-host"
              :port 12345
              :basis the-InetSocketAddress-object}}

## Multicast

### Sending Multicast packets

Multicast allows multiple sockets to receive the same packets. This
works by have them join a multicast group to which you can send
packets.

We will look at how to join a multicast group and receive packets in
the next section - for now, let's focus on how to send them. Sending
multicast packets is no different than sending normal datagram
packets.

The only difference is that you would pass in a multicast group
address to the send method:

    (-> (udp/socket)
        (udp/send "content" 1234 "230.0.0.1"))
        
All sockets that have joined the multicast group 230.0.0.1 will
receive the packet.
   
### Receiving Multicast packets

If you want to receive packets for a specific multicast group you need
to bind the socket by calling `vertx.datagram/listen`, then join the
multicast group by calling `vertx.datagram/join-multicast-group`.

This way you will be able to receive packets that were sent to the
address and port on which the socket listens and also to those sent to
the multicast group.

Beside this you also want to set a handler function which will be called for
each received packet.

So to listen on a specific address and port and also receive packets
for the Multicast group 230.0.0.1 you would do something like shown
here:

    (-> (udp/socket)
        (udp/listen 1234)
        (udp/on-data
          (fn [packet] ...))
        (udp/join-multicast-group "230.0.0.1"
          (fn [err socket]
            (if-not err 
              (println "join succeeded")))))

### Leaving a Multicast group 

There are sometimes situations where you want to receive packets for a
multicast group for a limited time.

In these situations you can first join a multicast group, then leave
the group later:

    (-> (udp/socket)
        (udp/listen 1234)
        (udp/on-data
          (fn [packet] ...))
        (udp/join-multicast-group "230.0.0.1"
          (fn [err socket]
            ;; only be part of the group for a second
            (core/timer 1000
              (udp/leave-multicast-group socket "230.0.0.1")))))

### Blocking multicast

It's also possible to block multicast for a specific sender address.

Be aware this only works on some operating systems and kernel
versions. Please check the Operating System documentation to see if
it's supported.

This an expert feature.

To block multicast from a specic address you can call
 `vertx.datagram/block-multicast-sender`:
 
    (-> (udp/socket)
        (udp/join-multicast-group "230.0.0.1")
        ;; This would block packets which are sent from 10.0.0.2
        (udp/block-multicast-sender "230.0.0.1" "10.0.0.2"))

## DatagramSocket properties

There are several properties you can set on a datagram socket:

* `:send-buffer-size` - the send buffer size in bytes.

* `:receive-buffer-size` - the TCP receive buffer size in bytes.

* `:reuse-address` - if true then addresses in TIME_WAIT state can be reused after
                     they have been closed.

* `:traffic-class` - ??

* `:broadcast` - controls the SO_BROADCAST socket option. When this option is set, 
                 Datagram (UDP) packets may be sent to a local interface's broadcast address.

* `:multicast-loopback-mode` - controls the IP_MULTICAST_LOOP socket option.
                               When this option is set, multicast packets will
                               also be received on the local interface. 

* `:multicast-time-to-live` - controls the IP_MULTICAST_TTL socket option. TTL stands for "Time to Live,"
                              but in this context it specifies the number of IP hops that a packet is
                              allowed to go through, specifically for multicast traffic. Each router
                              or gateway that forwards a packet decrements the TTL. If the TTL is
                              decremented to 0 by a router, it will not be forwarded.


You can set these properties at socket creation time by passing them
as a map to `vertx.datagram/socket`:

    (udp/socket :ipv4 {:broadcast true})
    
Or set them on an existing socket object, either by passing them as a
map to `vertx.utils/set-properties`:

    (utils/set-properties socket {:broadcast true})

Or by calling the corresponding java setter methods on the socket
object:

    (.setBroadcast socket true)


## DatagramSocket Local Address

You can find out the local address of the socket (i.e. the address of
 this side of the UDP Socket) by calling
 `vertx.datagram/local-address`. This will only return an address-map
 if the socket is actually listening.
    
## Closing a DatagramSocket

You can close a socket by passing it to the `vertx.datagram/close`
function.  This will close the socket and release all its resources.

<a id="flow-control"> </a> 
# Flow Control - Streams and Pumps

There are several objects in vert.x that allow data to be read from
and written to in the form of Buffers.

In Vert.x, calls to write data return immediately and writes are
internally queued.

It's not hard to see that if you write to an object faster than it can
actually write the data to its underlying resource then the write
queue could grow without bound - eventually resulting in exhausting
available memory.

To solve this problem a simple flow control capability is provided by
some objects in the vert.x API.

Any flow control aware object that can be written to is said to
implement `ReadStream`, and any flow control object that can be read
from is said to implement `WriteStream`.

Let's take an example where we want to read from a `ReadStream` and
write the data to a `WriteStream`.

A very simple example would be reading from a `NetSocket` on a server
and writing back to the same `NetSocket` - since `NetSocket`
implements both `ReadStream` and `WriteStream`, but you can do this
between any `ReadStream` and any `WriteStream`, including HTTP
requests and response, async files, WebSockets, etc.

A naive way to do this would be to directly take the data that's been
read and immediately write it to the NetSocket, for example:

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                ;; Write the data straight back
                (stream/write sock buffer)))))
        (net/listen 1234 "localhost"))

There's a problem with the above example: If data is read from the
socket faster than it can be written back to the socket, it will build
up in the write queue of the net socket, eventually running out of
RAM. This might happen, for example if the client at the other end of
the socket wasn't reading very fast, effectively putting back-pressure
on the connection.

Since `NetSocket` implements `WriteStream`, we can check if the
`WriteStream` is full before writing to it:

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                (if-not (.writeQueueFull sock)
                  (stream/write sock buffer))))))
        (net/listen 1234 "localhost"))

This example won't run out of RAM but we'll end up losing data if the
write queue gets full. What we really want to do is pause the
`NetSocket` when the write queue is full. Let's do that:

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                (if (.writeQueueFull sock)
                  (.pause sock)
                  (stream/write sock buffer))))))
        (net/listen 1234 "localhost"))
    
We're almost there, but not quite. The `NetSocket` now gets paused
when the file is full, but we also need to *unpause* it when the file
write queue has processed its backlog:

    (-> (net/server)
        (net/on-connect
          (fn [sock]
            (stream/on-data sock
              (fn [buffer]
                (if (.writeQueueFull sock)
                  (do 
                    (.pause sock)
                    (stream/on-drain #(.resume sock)))
                  (stream/write sock buffer))))))
        (net/listen 1234 "localhost"))
        
And there we have it. The `drain_handler` event handler will get
called when the write queue is ready to accept more data, this resumes
the `NetSocket` which allows it to read more data.

It's very common to want to do this when writing vert.x applications,
so we provide a helper function called `pump` which does all this hard
work for you. You just feed it the `ReadStream` and the `WriteStream`:

    (-> (net/server)
        (net/on-connect
          (stream/pump sock sock))
        (net/listen 1234 "localhost"))
        
Which does exactly the same thing as the more verbose example.

Let's look at the methods on `ReadStream` and `WriteStream` in more
detail:

## ReadStream

`ReadStream` is implemented by `AsyncFile`, `HttpClientResponse`,
`HttpServerRequest`, `WebSocket`, `NetSocket` and `SockJSSocket`.

Functions/methods:

* `vertx.stream/on-data`: set a handler which will receive data from the
  `ReadStream`. As data arrives the handler will be passed a Buffer.
* `.pause`: pause the handler. When paused no data will be received in
  the `data_handler`.
* `.resume`: resume the handler. The handler will be called if any
  data arrives.
* `vertx.stream/on-exception`: Will be called if an exception occurs
  on the `ReadStream`.
* `vertx.stream/on-end`: Will be called when end of stream is
  reached. This might be when EOF is reached if the `ReadStream`
  represents a file, or when end of request is reached if it's an HTTP
  request, or when the connection is closed if it's a TCP socket.

## WriteStream

`WriteStream` is implemented by `AsyncFile`, `HttpClientRequest`,
`HttpServerResponse`, `WebSocket`, `NetSocket` and `SockJSSocket`.

Functions/methods:

* `.write`: write a Buffer to the `WriteStream`. This method will
  never block. Writes are queued internally and asynchronously written
  to the underlying resource.
* `.setWriteQueueMaxSize`: set the number of bytes at which the write
  queue is considered *full*, and the function `write_queue_full?`
  returns `true`. Note that, even if the write queue is considered
  full, if `write` is called the data will still be accepted and
  queued.
* `.writeQueueFull`: returns `true` if the write queue is considered
  full.
* `vertx.stream/on-exception`: Will be called if an exception occurs
  on the `WriteStream`.
* `vertx.stream/on-drain`: The handler will be called if the
  `WriteStream` is considered no longer full.

## Pump

Instances of `Pump` (returned by `vertx.stream/pump`) have the
following methods:

* `.start`. Start the pump.
* `.stop`. Stops the pump. When the pump starts it is in stopped mode.
* `.setWriteQueueMaxSize`. This has the same meaning as
  `.setWriteQueueMaxSize` on the `WriteStream`.
* `.bytesPumped`. Returns total number of bytes pumped.

A pump can be started and stopped multiple times.

By default, `vertx.stream/pump` calls `.start` on the `Pump` before
returning. To prevent that, pass `false` as a third parameter:
   
    (stream/pump read-stream write-stream false)

# Writing HTTP Servers and Clients

## Writing HTTP servers

Vert.x allows you to easily write full featured, highly performant and
scalable HTTP servers.

### Creating an HTTP Server

To create an HTTP server you simply call `vertx.http/server`:

    (http/server)

### Start the Server Listening

To tell that server to listen for incoming requests you use the
`listen` function:

    (-> (http/server)
        (http/listen 8080 "myhost"))
    
The first parameter to `listen` is the port. The second parameter is
the hostname or ip address. If the hostname is omitted it will default
to `0.0.0.0` which means it will listen at all available interfaces.

The actual bind is asynchronous so the server might not actually be
listening until some time *after* the call to listen has returned. If
you want to be notified when the server is actually listening you can
provide a handler to the `listen` call. For example:

    (-> (http/server)
        (http/listen 8080
          (fn [err]
            (if-not err
              (println "Now listening!")))))


### Getting Notified of Incoming Requests

To be notified when a request arrives you need to set a request
handler. This is done by calling the `vertx.http/on-request` function,
passing in the handler:

    (-> (http/server)
        (http/on-request 
          (fn [request]
            (println "An HTTP request has been received")))
        (http/listen 8080))

This displays 'An HTTP request has been received!' every time an HTTP
request arrives on the server. You can try it by running the verticle
and pointing your browser at `http://localhost:8080`.

### Handling HTTP Requests

So far we have seen how to create an `HttpServer` and be notified of
requests. Lets take a look at how to handle the requests and do
something useful with them.

When a request arrives, the request handler is called passing in an
instance of `HttpServerRequest`. This object represents the server
side HTTP request.

The handler is called when the headers of the request have been fully
read. If the request contains a body, that body may arrive at the
server some time after the request handler has been called.

It contains functions to get the URI, path, request headers and
request parameters. It also contains a `response` property which is a
reference to an object that represents the server side HTTP response
for the object.

#### Request Method

The request object has a method called `.method` which returns the
String representing what HTTP method was requested. Possible values
for `method` are: `GET`, `PUT`, `POST`, `DELETE`, `HEAD`, `OPTIONS`,
`CONNECT`, `TRACE`, `PATCH`.

#### Request Version

The request object has a method `.version` which returns a string
representing the HTTP version.

#### Request URI

The request object has a property `.uri` which contains the full URI
(Uniform Resource Locator) of the request. For example, if the request
URI was:

    /a/b/c/page.html?param1=abc&param2=xyz    
    
Then `(.uri request)` would return the string
`/a/b/c/page.html?param1=abc&param2=xyz`.

Request URIs can be relative or absolute (with a domain) depending on
what the client sent. In most cases they will be relative.

The request uri contains the value as defined in
[Section 5.1.2 of the HTTP specification - Request-URI](http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html)

#### Request Path

The request object has a `.path` method which contains the path of the
request. For example, if the request URI was:

    /a/b/c/page.html?param1=abc&param2=xyz

Then `(.path request)` would return the string `/a/b/c/page.html`

#### Request Query

The request object has a `.query` method which contains the query of
the request. For example, if the request URI was:

    /a/b/c/page.html?param1=abc&param2=xyz

Then `(.query request)` would return the string
`param1=abc&param2=xyz`

#### Request Headers

The request headers are available using the `.headers` method on the
request object. The return value of the method is a `MultiMap`. A
MultiMap differs from a normal map in that it allows multiple values
with the same key.

You can also retrieve the headers as a Clojure map using the
`vertx.http/headers` function. Keys are converted to keywords, and
keys that have multiple values in the `MultiMap` will have those
values stored as a Vector in the Clojure map.

Here's an example that echoes the headers to the output of the
response. Run it and point your browser at `http://localhost:8080` to
see the headers.

    (require '[vertx.http :as http])
    
    (-> (http/server)
        (http/on-request
          #(-> %
               (http/server-response)
               (http/put-header "Content-Type" "text/plain")
               (http/end (pr-str (http/headers request)))))
        (http/listen 8080)

#### Request params

Similarly to the headers, the request parameters are available using
the `.params` method on the request object. The return value of the
method is a `MultiMap`. You can also use the `vertx.http/params`
function to retrieve the params as a Clojure map, using the same
conversion rules as `vertx.http/headers` above.

Request parameters are sent on the request URI, after the path. For
example if the URI was:

    /page.html?param1=abc&param2=xyz

Then the params map would look like:

    {:param1 "abc" :param2 "xyz"}

#### Remote Address

Use the method `.remoteAddress` to find out the address of the other
side of the HTTP connection. This will return an `InetSocketAddress`
object. Alternatively, you can use the `vertx.http/remote-address`
function to retrieve the remote address as a map of the form:

    {:host "127.0.0.1" :port 1234 :basis actual-inet-socket-address-object}

#### Absolute URI

Use the method `.absoluteURI` to return the absolute URI corresponding
to the request.

#### Reading Data from the Request Body

Sometimes an HTTP request contains a request body that we want to
read. As previously mentioned the request handler is called when only
the headers of the request have arrived so the `HttpServerRequest`
object does not contain the body. This is because the body may be very
large and we don't want to create problems with exceeding available
memory.

To receive the body, you set a handler function on the request object
via `vertx.stream/on-data`. This will then get called every time a
chunk of the request body arrives. Here's an example:

    (-> (http/server)
        (http/on-request
          #(stream/on-data %
            (fn [buf]
              (println "I received" (.length buf) "bytes"))))
        (http/listen 8080))
            
The data handler function may be called more than once depending on
the size of the body.

You'll notice this is very similar to how data from `NetSocket` is
read.

The request object implements the `ReadStream` interface so you can
pump the request body to a `WriteStream`.

In many cases, you know the body is not large and you just want to
receive it in one go. To do this you could do something like the
following:

    (-> (http/server)
        (http/on-request
          (fn [request]
            (let [body! (buf/buffer)]
              ;; Append the chunk to the buffer
              (stream/on-data request (partial buf/append! body!))
              ;; invoked when the entire body has been read
              (stream/on-end 
                #(println "The total body received was" (.length body!) "bytes")))))
        (http/listen 8080))
              
Like any `ReadStream` the end handler is invoked when the end of
stream is reached - in this case at the end of the request.

If the HTTP request is using HTTP chunking, then each HTTP chunk of
the request body will correspond to a single call of the data handler.

It's a very common use case to want to read the entire body before
processing it, so vert.x provides `vertx.http/on-body` for registering
a body handler.

The body handler is called only once when the *entire* request body
has been read.

*Beware of doing this with very large requests since the entire
 request body will be stored in memory.*

Here's an example using a body handler:

    (-> (http/server)
        (http/on-request
          #(http/on-body %
            (fn [buf]
              (println "The total body received" (.length buf) "bytes"))))
        (http/listen 8080))   
        
#### Handling Multipart Form Uploads

Vert.x understands file uploads submitted from HTML forms in
browsers. In order to handle file uploads you should set the upload
handler on the request via `vertx.http/on-upload`. The handler will be
called once for each upload in the form. The value passed to the
handler is a map of properties for the file:

* :filename     - the name of the file
* :name         - the name of the upload attribute
* :content-type - the content-type specified in the upload
* :encoding     - the content transfer encoding
* :size         - the size of the file in bytes
* :charset      - the Charset as a String
* :basis        - the underlying `HttpServerFileUpload` object, which is also a `ReadStream`
* :save-fn      - a single-arity fn that can be passed a path to save the file to disk

Example:

    (http/on-upload request
      (fn [upload]
       (println "Got an upload" (:filename upload))))

The `HttpServerFileUpload` object (available as `:basis`) is a
`ReadStream` so you read the data and stream it to any object that
implements `WriteStream` using a Pump, as previously discussed.

You can also stream it directly to disk using the convenience function
available as `:save-fn`:

    (http/on-upload request
      (fn [upload]
       ((:save-fn upload) (format "uploads/%s" (:filename upload)))))
       
#### Handling Multipart Form Attributes

If the request corresponds to an HTML form that was submitted you can
use the`.formAttributes` method on the request object to retrieve the
form attributes. The return value of the method is a `MultiMap`. You
can also use the `vertx.http/form-attributes` function to retrieve the
attributes as a Clojure map, using the same conversion rules as
`vertx.http/headers` above.

This should only be called after *all* of the request has been read -
this is because form attributes are encoded in the request *body* not
in the request headers. You must also call
`vertx.http/expect-multi-part` on the request *before* any of the body
is read in order for the form attributes to be available.

    (stream/on-end request
      (http/expect-multi-part request)
      ;; The request has been full read, so now we can look at the form attributes
      #(do-something-with-form-attributes (http/form-attributes request)))
      
### HTTP Server Responses

As previously mentioned, you can use `vertx.http/server-response` to
create an HTTP response object from an HTTP request. You use it to
write the response back to the client.

### Setting Status Code and Message

To set the HTTP status code for the response use the `:status-code`
property. You can also use the `:status-message` property to set the
status message. If you do not set the status message a default message
will be used. You can pass these properties as a map to
`vertx.http/server-response`, set them on an existing response object
with `vertx.utils/set-properties`, or call the corresponding java
setter methods (`.setStatusCode`, `.setStatusMessage`).

    (-> (http/server)
        (http/on-request
          #(-> %
               (http/server-response 
                 {:status-code 404
                  :status-message "Too many gerbils"})
               http/end))
        (http/listen 8080))
    
The default value for `:status-code` is `200`.

#### Writing HTTP responses

To write data to an HTTP response, you invoke the `vertx.stream/write`
function. This function can be invoked multiple times before the
response is ended. `write` can either take anything bufferable, or a
String and encoding:

Example:

    (-> request
        (stream/write "a string") ;; encodes as UTF-8 by default
        (stream/write 42)
        (stream/write some-buffer!)
        (stream/write "another string" "UTF-16"))

The write methods are asynchronous and always returns immediately
after the write has been queued.

If you are just writing a single string or bufferable to the HTTP
response you can write it and end the response in a single call to the
`vertx.http/end` function.

The first call to `write` results in the response header being being
written to the response.

Consequently, if you are not using HTTP chunking then you must set the
`Content-Length` header before writing to the response, since it will
be too late otherwise. If you are using HTTP chunking you do not have
to worry.

#### Ending HTTP responses

Once you have finished with the HTTP response you must call the
`vertx.http/end` function on it.

This function can be invoked in several ways:

With no arguments, the response is simply ended.

    (http/end response)

The function can also be called with a string or bufferable in the
same way `vertx.stream/write` is called. In this case it's just the
same as calling `write` with a string or bufferable followed by
calling `end` with no arguments. For example:

    
    (http/end response "That's all folks")

#### Closing the underlying connection

You can close the underlying TCP connection of the request by calling
the `.close` method:

    (.close response)

#### Response headers

HTTP response headers can be added to the response by passing them to
`vertx/http/add-header`:

    (-> response
        (http/add-header "Some-header" "foo")
        (http/add-header "Another-header" "bar"))

Response headers must all be added before any parts of the response
body are written.

#### Chunked HTTP Responses and Trailers

Vert.x supports
[HTTP Chunked Transfer Encoding](http://en.wikipedia.org/wiki/Chunked_transfer_encoding). This
allows the HTTP response body to be written in chunks, and is normally
used when a large response body is being streamed to a client, whose
size is not known in advance.

You put the HTTP response into chunked mode by setting the `:chunked`
property.

    (http/server-response request {:chunked true})
    
    ;; or
    
    (utils/set-property response :chunked true)
    
    ;; or
    
    (.setChunked response true)

Default is non-chunked. When in chunked mode, each call to
`vertx.stream/write` on a response will result in a new HTTP chunk
being written out.

When in chunked mode you can also write HTTP response trailers to the
response. These are actually written in the final chunk of the
response.

HTTP response trailers can be added to the response by passing them to
`vertx/http/add-trailer`:

    (-> response
        (http/add-trailer "Some-trailer" "foo")
        (http/add-trailer "Another-trailer" "bar"))


### Serving files directly from disk

If you were writing a web server, one way to serve a file from disk
would be to open it as an `AsyncFile` and pump it to the HTTP
response. Or you could load it it one go using the file system API and
write that to the HTTP response.

Alternatively, vert.x provides a method which allows you to serve a
file from disk to an HTTP response in one operation. Where supported
by the underlying operating system this may result in the OS directly
transferring bytes from the file to the socket without being copied
through userspace at all.

Using `vertx.http/send-file` is usually more efficient for large
files, but may be slower than using `vertx.filesystem/read-file` to
manually read the file as a buffer and write it directly to the
response.

To do this use the `send-file` function on the HTTP response. Here's a
simple HTTP web server that serves static files from the local `web`
directory:

    (-> (http/server)
        (http/on-request
          (fn [req]
            (-> req
                http/server-response
                (http/send-file 
                  (str "web/"
                    (let [path (.path req)]
                      (cond 
                        (= path "/") "index.html"
                        (not (re-find #"\.\.")) path
                        :default "error.html"))))))
        (http/listen 8080))
                    
There's also a variant of `send-file` which takes the name of a file
to serve if the specified file cannot be found:

    (http/send-file response (str "web/" file) 
      :not-found "handler_404.html")

*Note: If you use `send-file` while using HTTPS it will copy through
 userspace, since if the kernel is copying data directly from disk to
 socket it doesn't give us an opportunity to apply any encryption.*

**If you're going to write web servers using vert.x be careful that
  users cannot exploit the path to access files outside the directory
  from which you want to serve them.**

### Pumping Responses

Since the HTTP Response implements `WriteStream` you can pump to it
from any `ReadStream`, e.g. an `AsyncFile`, `NetSocket` or
`HttpServerRequest`.

Here's an example which echoes HttpRequest headers and body back in
the HttpResponse. It uses a pump for the body, so it will work even if
the HTTP request body is much larger than can fit in memory at any one
time:

    (-> (http/server)
        (on-request 
          (fn [req]
            (let [response (http/server-response req)]
              (http/add-headers response (http/headers req))
              (stream/pump req response)
              (stream/on-end req (partial http/end response)))))
        (listen 8080))
        
## Writing HTTP Clients

### Creating an HTTP Client

To create an HTTP client, call `vertx.http/client`:

    (http/client)

You set the port and hostname (or ip address) that the client will
connect to using the `:host` and `:port` properties:

    (http/client {:port 8181 :host "foo.com"})
    
A single `HttpClient` always connects to the same host and port. If
you want to connect to different servers, create more instances.

The default port is `80` and the default host is `localhost`. So if
you don't explicitly set these values that's what the client will
attempt to connect to.

### Pooling and Keep Alive

By default the `HttpClient` pools HTTP connections. As you make
requests a connection is borrowed from the pool and returned when the
HTTP response has ended.

If you do not want connections to be pooled you can set `:keep-alive`
to `false`:

    (http/client {:keep-alive false})

In this case a new connection will be created for each HTTP request
and closed once the response has ended.

You can set the maximum number of connections that the client will
pool as follows:

    (http/client {:max-pool-size 10})
    
The default value is `1`.

### Closing the client

Vert.x will automatically close any clients when the verticle is
stopped, but if you want to close it explicitly you can:

    (.close client)

### Making Requests

To make a request using the client you invoke one the functions named
after the HTTP method that you want to invoke.

For example, to make a `POST` request:

    (-> (http/client {:host "foo.com"})
        (http/post "/some-path/"
          #(println "got response" (.statusCode %)))
        (http/end))
        
To make a PUT request use the `put` function, to make a GET request use
the `get` function, etc.

Legal request functions are: `get`, `put`, `post`, `delete`, `head`,
`options`, `connect`, `trace` and `patch`.

The general modus operandi is you invoke the appropriate method
passing in the request URI as the first parameter, the second
parameter is an event handler which will get called when the
corresponding response arrives. The response handler is passed the
client response object as an argument.

The value specified in the request URI corresponds to the Request-URI
as specified in
[Section 5.1.2 of the HTTP specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html). In
most cases it will be a relative URI.

*Please note that the domain/port that the client connects to is
 determined by the properties `:port` and `:host`, and is not parsed
 from the uri.*

The return value from the appropriate request method is an
`HttpClientRequest` object. You can use this to add headers to the
request, and to write to the request body. The request object
implements `WriteStream`.

Once you have finished with the request you must call the
`vertx.http/end` function.

If you don't know the name of the request method in advance there is a
general `request` function which takes the HTTP method as a parameter:

    (-> (http/client {:host "foo.com"})
        (http/request :POST "/some-path/"
          #(println "got response" (.statusCode %)))
        (http/end))
        
There is also a function called `get-now` which does the same as
`get`, but automatically ends the request. This is useful for simple
GETs which don't have a request body:

    (-> (http/client {:host "foo.com"})
        (http/get-now "/some-path/"
          #(println "got response" (.statusCode %)))
        (http/end))


#### Handling exceptions

You can set an exception handler on the `HttpClient` class and it will
receive all exceptions for the client unless a specific exception
handler has been set on a specific `HttpClientRequest` object.

#### Writing to the request body

Writing to the client request body has a very similar API to writing
to the server response body.

To write data to an HTTP request, you invoke the `vertx.stream/write`
function. This function can be invoked multiple times before the
request is ended. `write` can either take anything bufferable, or a
String and encoding:

Example:

    (-> request
        (stream/write "a string") ;; encodes as UTF-8 by default
        (stream/write 42)
        (stream/write some-buffer!)
        (stream/write "another string" "UTF-16"))

The write methods are asynchronous and always returns immediately
after the write has been queued.

If you are just writing a single string or bufferable to the HTTP
request you can write it and end the request in a single call to the
`vertx.http/end` function.

The first call to `write` results in the request header being being
written to the request.

Consequently, if you are not using HTTP chunking then you must set the
`Content-Length` header before writing to the request, since it will
be too late otherwise. If you are using HTTP chunking you do not have
to worry.

#### Ending HTTP responses

Once you have finished with the HTTP request you must call the
`vertx.http/end` function on it.

This function can be invoked in several ways:

With no arguments, the request is simply ended.

    (http/end request)

The function can also be called with a string or bufferable in the
same way `vertx.stream/write` is called. In this case it's just the
same as calling `write` with a string or bufferable followed by
calling `end` with no arguments. For example:

    
    (http/end request "That's all folks")


#### Writing Request Headers

HTTP request headers can be added to the request by passing them to
`vertx/http/add-header`:

    (-> request
        (http/add-header "Some-header" "foo")
        (http/add-header "Another-header" "bar"))

Request headers must all be added before any parts of the request
body are written.

#### Request timeouts

You can set a timeout for specific Http Request using the
`.setTimeout` method. If the request does not return any data within
the timeout period an exception will be passed to the exception
handler (if provided) and the request will be closed.

#### HTTP chunked requests

Vert.x supports
[HTTP Chunked Transfer Encoding](http://en.wikipedia.org/wiki/Chunked_transfer_encoding)
for requests. This allows the HTTP request body to be written in
chunks, and is normally used when a large request body is being
streamed to the server, whose size is not known in advance.

You put the HTTP request into chunked mode by calling the method `.setChunked`.

    (.setChunked request true)

Default is non-chunked. When in chunked mode, each write to the
request will result in a new HTTP chunk being written out.

### HTTP Client Responses

Client responses are received as an argument to the response handler
block that is specified when making a request.

The response object implements `ReadStream`, so it can be pumped to a
`WriteStream` like any other `ReadStream`.

To query the status code of the response use the `.statusCode`
method. The `.statusMessage` method contains the status message. For
example:

    (-> (http/client {:host "foo.com"})
        (http/get-now "/some-path"
          (fn [resp]
            (println "server returned status code:" (.statusCode resp))
            (println "server returned status message:" (.statusMessage resp)))))

#### Reading Data from the Response Body

The API for reading a http client response body is very similar to the
API for reading a http server request body.

Sometimes an HTTP response contains a request body that we want to
read. Like an HTTP request, the client response handler is called when
all the response headers have arrived, not when the entire response
body has arrived.

To receive the response body, you set a data handler on the response
object using `vertx.stream/on-data` which gets called as parts of the
HTTP response arrive. Here's an example:

    (-> (http/client {:host "foo.com"})
        (http/get-now "/some-path"
          (fn [resp]
            (stream/on-data resp
              (fn [buf] 
                (println "I received" (.length buf) "bytes"))))))


The response object implements the `ReadStream` interface so you can
pump the response body to a `WriteStream`. See the chapter on streams
and pump for a detailed explanation.

The data handler function can be called multiple times for a single
HTTP response.

As with a server request, if you wanted to read the entire response
body before doing something with it you could do something like the
following:

    (-> (http/client {:host "foo.com"})
        (http/get-now "/some-path"
          (fn [resp]
            (let [body! (buf/buffer)]
              (stream/on-data resp
                (partial buf/append! body!))
              (stream/on-end resp
                #(println "The total body received was" (.length body!)))))))

Like any `ReadStream` the end handler is invoked when the end of
stream is reached - in this case at the end of the response.

If the HTTP response is using HTTP chunking, then each chunk of the
response body will correspond to a single call to the data handler
function.

It's a very common use case to want to read the entire body in one go,
so vert.x allows a body handler function to be set on the response
object via `vertx.http/on-body`.

The body handler is called only once when the *entire* response body
has been read.

*Beware of doing this with very large responses since the entire
 response body will be stored in memory.*

Here's an example using `body_handler`:

    (-> (http/client {:host "foo.com"})
        (http/get-now "/some-path"
          (fn [resp]
            (stream/on-body resp
              #(println "The total body received was" (.length %))))))

#### Reading cookies

You can read the list of cookies from the response using the method
`.cookies`.


### 100-Continue Handling

According to the
[HTTP 1.1 specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html)
a client can set a header `Expect: 100-Continue` and send the request
header before sending the rest of the request body.

The server can then respond with an interim response status `Status:
100 (Continue)` to signify the client is ok to send the rest of the
body.

The idea here is it allows the server to authorise and accept/reject
the request before large amounts of data is sent. Sending large
amounts of data if the request might not be accepted is a waste of
bandwidth and ties up the server in reading data that it will just
discard.

Vert.x allows you to set a continue handler on the client request
object via `vertx.http/on-continue`. This will be called if the server
sends back a `Status: 100 (Continue)` response to signify it is ok to
send the rest of the request.

This is used in conjunction with the `.sendHead` method to send the
head of the request.

An example will illustrate this:

    (let [request (-> (http/client {:host "foo.com})
                      (http/get-now "/some-path"
                        #(println "Got a response:" (.statusCode %))))]
      (http/add-header request "Expect" "100-Continue")
      (.setChunked request true)
      (http/on-continue request
        (-> request
            (stream/write "Some Data")
            http/end))
      (.sendHead request))

### HTTP Compression

Vert.x comes with support for HTTP Compression out of the box. Which means the
HTTPClient can let the remote Http server know that it supports compression,
and so will be able to handle compressed response bodies. A Http server is
free to either compress with one of the supported compression algorithm or send
the body back without compress it at all. So this is only a hint for the Http
server which it may ignore at all.

To tell the Http server which compression is supported by the `HttpClient` it
will include a 'Accept-Encoding' header with the supported compression algorithm
as value. Multiple compression algorithms are supported. In case of Vert.x this
will result in have the following header added:

    Accept-Encoding: gzip, deflate

The Http Server will choose then from one of these. You can detect if a HttpServer
did compress the body by checking for the 'Content-Encoding' header in the
response sent back from it.
If the body of the response was compressed via gzip it will include for example
the following header:

    Content-Encoding: gzip

To enable compression you only need to do:

    (vertx.http/client {:port 8080 :host "127.0.0.1" :try-use-compression true})

## Pumping Requests and Responses

The HTTP client and server requests and responses all implement either
`ReadStream` or `WriteStream`. This means you can pump between them
and any other read and write streams.

## HTTPS Servers

HTTPS servers are very easy to write using vert.x.

An HTTPS server has an identical API to a standard HTTP
server. Getting the server to use HTTPS is just a matter of
configuring the HTTP Server before `listen` is called.

Configuration of an HTTPS server is done in exactly the same way as
configuring a `NetServer` for SSL. Please see the SSL server chapter
for detailed instructions.

## HTTPS Clients

HTTPS clients can also be very easily written with vert.x

Configuring an HTTP client for HTTPS is done in exactly the same way
as configuring a `NetClient` for SSL. Please see the SSL client
chapter for detailed instructions.

## Scaling HTTP servers

Scaling an HTTP or HTTPS server over multiple cores is as simple as
deploying more instances of the verticle. For example:

    vertx runmod com.mycompany~my-mod~1.0 -instance 20

Or, for a raw verticle:

    vertx run foo.MyServer -instances 20
    
The scaling works in the same way as scaling a `NetServer`. Please see
the chapter on scaling Net Servers for a detailed explanation of how
this works.

# Routing HTTP requests with Pattern Matching

Vert.x lets you route HTTP requests to different handlers based on
pattern matching on the request path. It also enables you to extract
values from the path and use them as parameters in the request.

This is particularly useful when developing REST-style web
applications.

To do this you simply create a `RouteMatcher` using the functions in
the `vertx.http.route` namespace and use it as handler in an HTTP
server. See the chapter on HTTP servers for more information on
setting HTTP handlers.

## Specifying matches.

You can add different matches to a route matcher. For example, to send
all GET requests with path `/animals/dogs` to one handler and all GET
requests with path `/animals/cats` to another handler you would do:

    (-> (http/server)
        (http/on-request
           (-> (route/get "/animals/dogs"
                 (fn [req]
                   (-> (http/server-response req)
                       (http/end "You requested dogs"))))
               (route/get "/animals/cats"
                 (fn [req]
                   (-> (http/server-response req)
                       (http/end "You requested cats"))))))
        (http/listen 8080))
        
Corresponding functions exist for each HTTP method - `get`, `post`,
`put`, `delete`, `head`, `options`, `trace`, `connect` and `patch`.

There's also an `all` function which applies the match to any HTTP
request method.

All route functions return a route matcher object and optionally take
a route matcher as the first argument, allowing them to be
chained. You can create a virgin route matcher with
`vertx.http.route/matcher`.

The handler specified to the method is just a normal HTTP server
request handler, the same as you would supply to the `on-request`
function.

You can provide as many matches as you like and they are evaluated in
the order you added them, the first matching one will receive the
request.

A request is sent to at most one handler.

## Extracting parameters from the path

If you want to extract parameters from the path, you can do this too,
by using the `:` (colon) character to denote the name of a
parameter. For example:

    (-> (http/server)
        (http/on-request
          (route/put "/:blogname/:post"
            (fn [req]
              (let [params (http/params req)]
                (-> (http/server-response req)
                    (http/end (format "blogname is %s and post is %s"
                                 (:blogname params)
                                 (:post params))))))))
        (http/listen 8080))

Any params extracted by pattern matching are added to the map of
request parameters.

In the above example, a PUT request to "/myblog/post1" would result in
the param `:blogname` getting the value `"myblog"` and the param
`:post` getting the value "post1".

Valid parameter names must start with a letter of the alphabet and be
followed by any letters of the alphabet or digits or underscore
character.

## Extracting params using Regular Expressions

Regular Expressions can be used to extract more complex matches. In
this case capture groups are used to capture any parameters.

Since the capture groups are not named they are added to the request
with names `param0`, `param1`, `param2`, etc.

To use regular expressions, simply pass them to the same functions
above instead of pattern strings.

For example:

    (-> (http/server)
        (http/on-request
          (route/all #"\/([^\/]+)\/([^\/]+)"
            (fn [req]
              (let [params (http/params req)]
                (-> (http/server-response req)
                    (http/end (format "first is %s and second is %s"
                                 (:param0 params)
                                 (:param1 params))))))))
        (http/listen 8080))
        
Run the above and point your browser at
`http://localhost:8080/animals/cats`.

It will display 'first is animals and second is cats'.    

## Handling requests where nothing matches

You can use the `no-match` function to specify a handler that will be
called if nothing matches. If you don't specify a no match handler and
nothing matches, a 404 will be returned.

    (route/no-match 
      (fn [req]
        (http/end (server-response req) "Nothing matched")))
    
# WebSockets 


[WebSockets](http://en.wikipedia.org/wiki/WebSocket) are a web
technology that allows a full duplex socket-like connection between
HTTP servers and HTTP clients (typically browsers).

## WebSockets on the server

To use WebSockets on the server you create an HTTP server as normal,
but instead of setting a request handler you set a websocket handler
on the server via `vertx.http.websocket/on-websocket`.

    (-> (http/server)
        (ws/on-websocket 
          (fn [ws]
            ;; A WebSocket has connected!
            ))
        (http/listen 8080))
            
### Reading from and Writing to WebSockets

The `websocket` instance passed into the handler implements both
`ReadStream` and `WriteStream`, so you can read and write data to it
in the normal ways. I.e by setting a data handler and calling the
`vertx.stream/write` function.

See the chapter on `NetSocket` and streams and pumps for more
information.

For example, to echo all data received on a WebSocket:

    (-> (http/server)
        (ws/on-websocket 
          #(stream/pump % %))
        (listen 8080))

The `vertx.http.websocket` namespace provides the functions
`write-binary-frame` for writing binary data, and `write-text-frame`
for writing text data.

### Rejecting WebSockets

Sometimes you may only want to accept WebSockets which connect at a
specific path.

To check the path, you can query the `.path` method of the
websocket. You can then call the `.reject` method to reject the
websocket.

    (-> (http/server)
        (ws/on-websocket 
          (fn [ws]
            (if (= "/services/echo" (.path ws))
              (stream/pump ws ws)
              (.reject ws))))
        (http/listen 8080))
        
### Event Bus Write Handler

Like NetSockets, every WebSocket automatically registers handlers on
the event bus, and when any buffers are received in those handlers, it
writes them to itself. This enables you to write data to a WebSocket
which is potentially in a completely different verticle or even in a
different Vert.x instance by sending the buffer to the address of that
handler.

There are two handlers registered for each WebSocket: one for text
data, the other for binary. The address of each handler is provided by
the `.textHandlerID` and `.binaryHandlerID` methods, respectively.

### Headers on the websocket

You can use the `vertx.http/headers` function to retrieve the headers
passed in the Http Request from the client that caused the upgrade to
websockets.

## WebSockets on the HTTP client

To use WebSockets from the HTTP client, you create the HTTP client as
normal, then call the `vertx.http.websocket/connect` function, passing
in the URI that you wish to connect to at the server, and a handler.

The handler will then get called if the WebSocket successfully
connects. If the WebSocket does not connect - perhaps the server
rejects it, then any exception handler on the HTTP client will be
called.

Here's an example of WebSockets on the client:

    (-> (http/client {:host "foo.com" :port 8080})
        (ws/connect "/services/echo"
          (fn [ws]
            (stream/on-data ws #(partial println "got"))
            (ws/write-text-frame ws "foo"))))
              
Note that the host (and port) is set on the `HttpClient` instance, and
the uri passed in the connect is typically a *relative* URI.
    
Again, the client side WebSocket implements `ReadStream` and
`WriteStream`, so you can read and write to it in the same way as any
other stream object.

## WebSockets in the browser

To use WebSockets from a compliant browser, you use the standard
WebSocket API. Here's some example client side JavaScript which uses a
WebSocket.

    <script>

        var socket = new WebSocket("ws://localhost:8080/services/echo");

        socket.onmessage = function(event) {
            alert("Received data from websocket: " + event.data);
        }

        socket.onopen = function(event) {
            alert("Web Socket opened");
            socket.write("Hello World");
        };

        socket.onclose = function(event) {
            alert("Web Socket closed");
        };

    </script>

For more information see the
[WebSocket API documentation](http://dev.w3.org/html5/websockets/)

### Set a max frame size for WebSockets

To set the max frame size for WebSocket, you will need to set it as a
property on the http server:

    (http/server
    {:host "foo.com" :port 8080 :max-web-socket-frame-size 1024})

or client:

    (http/client
    {:host "foo.com" :port 8080 :max-web-socket-frame-size 1024})

## Routing WebSockets with Pattern Matching

**TODO**

# SockJS

WebSockets are a new technology, and many users are still using
browsers that do not support them, or which support older, pre-final,
versions.

Moreover, WebSockets do not work well with many corporate
proxies. This means that's it's not possible to guarantee a WebSocket
connection is going to succeed for every user.

Enter SockJS.

SockJS is a client side JavaScript library and protocol which provides
a simple WebSocket-like interface to the client side JavaScript
developer irrespective of whether the actual browser or network will
allow real WebSockets.

It does this by supporting various different transports between
browser and server, and choosing one at runtime according to browser
and network capabilities. All this is transparent to you - you are
simply presented with the WebSocket-like interface which *just works*.

Please see the
[SockJS website](https://github.com/sockjs/sockjs-client) for more
information.

## SockJS Server

Vert.x provides a complete server side SockJS implementation.

This enables vert.x to be used for modern, so-called *real-time* (this
is the *modern* meaning of *real-time*, not to be confused by the more
formal pre-existing definitions of soft and hard real-time systems)
web applications that push data to and from rich client-side
JavaScript applications, without having to worry about the details of
the transport.

To create a SockJS server you simply create a HTTP server as normal
and pass it in to function that creates a SockJS server.

    (require `[vertx.http :as http]
             `[vertx.http.sockjs :as sockjs])
             
    (-> (http/server) (sockjs/sockjs-server))

Each SockJS server can host multiple *applications*.

Each application is defined by some configuration, and provides a
handler which gets called when incoming SockJS connections arrive at
the server.

For example, to create a SockJS echo application:

    (let [server (http/server)]
       (-> (sockjs/sockjs-server server)
           (sockjs/install-app {:prefix "/echo"}
             #(stream/on-data % (partial stream/write %))))
       (http/listen server 8080 "localhost"))

The configuration can take the following fields:

* `:prefix`: A url prefix for the application. All http requests whose
  paths begins with selected prefix will be handled by the
  application. This property is mandatory.
* `:insert_JSESSIONID`: Some hosting providers enable sticky sessions
  only to requests that have JSESSIONID cookie set. This setting
  controls if the server should set this cookie to a dummy value. By
  default setting JSESSIONID cookie is enabled. More sophisticated
  behaviour can be achieved by supplying a function.
* `:session_timeout`: The server sends a `close` event when a client
  receiving connection have not been seen for a while. This delay is
  configured by this setting. By default the `close` event will be
  emitted when a receiving connection wasn't seen for 5 seconds.
* `:heartbeat_period`: In order to keep proxies and load balancers
  from closing long running http requests we need to pretend that the
  connecion is active and send a heartbeat packet once in a
  while. This setting controlls how often this is done. By default a
  heartbeat packet is sent every 5 seconds.
* `:max_bytes_streaming`: Most streaming transports save responses on
  the client side and don't free memory used by delivered
  messages. Such transports need to be garbage-collected once in a
  while. `:max_bytes_streaming` sets a minimum number of bytes that can
  be send over a single http streaming request before it will be
  closed. After that client needs to open new request. Setting this
  value to one effectively disables streaming and will make streaming
  transports to behave like polling transports. The default value is
  128K.
* `:library_url`: Transports which don't support cross-domain
  communication natively ('eventsource' to name one) use an iframe
  trick. A simple page is served from the SockJS server (using its
  foreign domain) and is placed in an invisible iframe. Code run from
  this iframe doesn't need to worry about cross-domain issues, as it's
  being run from domain local to the SockJS server. This iframe also
  does need to load SockJS javascript client library, and this option
  lets you specify its url (if you're unsure, point it to the latest
  minified SockJS client release, this is the default). The default
  value is `http://cdn.sockjs.org/sockjs-0.3.4.min.js`

## Reading and writing data from a SockJS server

The object passed into the SockJS handler implements `ReadStream` and
`WriteStream` much like `NetSocket` or `WebSocket`. You can therefore
use the standard API for reading and writing to the SockJS socket or
using it in pumps. See the chapter on Streams and Pumps for more
information.

    (let [server (http/server)]
       (-> (sockjs/sockjs-server server)
       (sockjs/install-app {:prefix "/echo"}
         #(stream/on-data % (partial stream/write %))))
       (http/listen server 8080 "localhost"))

## SockJS client

For full information on using the SockJS client library please see the
SockJS website. A simple example:

    <script>
       var sock = new SockJS('http://mydomain.com/my_prefix');

       sock.onopen = function() {
           console.log('open');
       };

       sock.onmessage = function(e) {
           console.log('message', e.data);
       };

       sock.onclose = function() {
           console.log('close');
       };
    </script>
    
As you can see the API is very similar to the WebSockets API.    

# SockJS - EventBus Bridge

## Setting up the Bridge

By connecting up SockJS and the vert.x event bus we create a
distributed event bus which not only spans multiple vert.x instances
on the server side, but can also include client side JavaScript
running in browsers.

We  can therefore  create  a huge  distributed  bus encompassing  many
browsers and servers.  The browsers don't have to be  connected to the
same server as long as the servers are connected.

On the server side we have already discussed the event bus API.

We also provide a client side JavaScript library called `vertxbus.js`
which provides the same event bus API, but on the client side.

This library internally uses SockJS to send and receive data to a
SockJS vert.x server called the SockJS bridge. It's the bridge's
responsibility to bridge data between SockJS sockets and the event bus
on the server side.

Creating a Sock JS bridge is simple. You just call the
`vertx.http.sockjs/bridge` function with the SockJS server instance.

You will also need to secure the bridge (see below).

The following example creates and starts a SockJS bridge which will
bridge any events sent to the path `eventbus` on to the server side
event bus.

    (let [http-server (http/server)]
      (sockjs/bridge 
        (sockjs/sockjs-server http-server)
        {:prefix "/eventbus"} [{}] [{}]) 
      (http/listen http-server 8080 "localhost"))

## Using the Event Bus from client-side JavaScript

Once you've set up a bridge, you can use the event bus from the client
side as follows:

In your web page, you need to load the script `vertxbus.js`, then you
can access the vert.x event bus API. Here's a rough idea of how to use
it. For a full working examples, please consult the bundled examples.

    <script src="http://cdn.sockjs.org/sockjs-0.3.4.min.js"></script>
    <script src='vertxbus.js'></script>

    <script>

        var eb = new vertx.EventBus('http://localhost:8080/eventbus');
        
        eb.onopen = function() {
        
          eb.register_handler('some-address', function(message) {

            console.log('received a message: ' + JSON.stringify(message);

          });

          eb.send('some-address', {name: 'tim', age: 587});
        
        }
       
    </script>

You can find `vertxbus.js` in the `client` directory of the vert.x
distribution.

The first thing the example does is to create a instance of the event
bus:

    var eb = new vertx.EventBus('http://localhost:8080/eventbus')

The parameter to the constructor is the URI where to connect to the
event bus. Since we create our bridge with the prefix `eventbus` we
will connect there.

You can't actually do anything with the bridge until it is
opened. When it is open the `onopen` handler will be called.

The client side event bus API for registering and unregistering
handlers and for sending messages is exactly the same as the server
side one. Please consult the JavaScript core manual chapter on the
EventBus for a description of that API.

**There is one more thing to do before getting this working, please read the "Securing the Bridge" section**

## Using the Event Bus from client-side ClojureScript

If you are writing your client-side JavaScript as ClojureScript, you
can use the ClojureScript EventBus wrapper that ships with the Clojure
language module. Here is the example from above, but in ClojureScript
using the wrapper:

    (ns demo.client
      (:require [vertx.client.eventbus :as eb]))
      
    (def eb (atom nil))
    
    (defn register-handler [] 
      (eb/on-message @eb "some-address"
        #(.log js/console (str "received a message: " %))))
        
    (reset! eb (eb/eventbus "http://localhost:8080/eventbus"))
    (eb/on-open @eb register-handler)
    (eb/on-open @eb #(eb/send @eb "some-address" {:name "tim" :age 587}))

`eventbus.cljs` is included in the `io.vertx/clojure-api` jar, and
includes a `deps.cljs` that brings in `vertxbus.js` and `sockjs.js` as
foreign libs, so there's no need to include them in your html.

The API is similar to the server-side EventBus API, but has some
differences. Refer to
[the source](https://github.com/vert-x/mod-lang-clojure/blob/master/api/src/main/clojure/vertx/client/eventbus.cljs)
to see the full client-side API.


## Securing the Bridge

If you started a bridge like in the above example without securing it,
and attempted to send messages through it you'd find that the messages
mysteriously disappeared. What happened to them?

For most applications you probably don't want client side JavaScript
being able to send just any message to any verticle on the server side
or to all other browsers.

For example, you may have a persistor verticle on the event bus which
allows data to be accessed or deleted. We don't want badly behaved or
malicious clients being able to delete all the data in your database!
Also, we don't necessarily want any client to be able to listen in on
any topic.

To deal with this, a SockJS bridge will, by default refuse to let
through any messages. It's up to you to tell the bridge what messages
are ok for it to pass through. (There is an exception for reply
messages which are always allowed through).

In other words the bridge acts like a kind of firewall which has a
default *deny-all* policy.

Configuring the bridge to tell it what messages it should pass through
is easy. You pass in two arrays of JSON objects (represented by maps)
that represent *matches*, as the final argument in the call to
`bridge`.

The first array is the *inbound* list and represents the messages that
you want to allow through from the client to the server. The second
array is the *outbound* list and represents the messages that you want
to allow through from the server to the client.

Each match can have up to three fields:

1. `:address`: This represents the exact address the message is being
   sent to. If you want to filter messages based on an exact address
   you use this field.
2. `:address_re`: This is a regular expression that will be matched
   against the address. If you want to filter messages based on a
   regular expression you use this field. If the `address` field is
   specified this field will be ignored.
3. `:match`: This allows you to filter messages based on their
   structure. Any fields in the match must exist in the message with
   the same values for them to be passed. This currently only works
   with JSON messages.

When a message arrives at the bridge, it will look through the
available permitted entries.

* If an `:address` field has been specified then the `:address` must
  match exactly with the address of the message for it to be
  considered matched.

* If an `:address` field has not been specified and an `:address_re`
  field has been specified then the regular expression in `:address_re`
  must match with the address of the message for it to be considered
  matched.

* If a `:match` field has been specified, then also the structure of
  the message must match.


Here is an example:

    (let [http-server (http/server)
          sockjs-server 
          auth-inbound [
                     ;;Let through any messages sent to 'demo.orderMgr'
                     {:address "demo.orderMgr"}
                     
                     ;; Allow calls to the address 'demo.persistor' as long as the messages
                     ;; have an action field with value 'find' and a collection field with value
                     ;; 'albums'
                     {:address "demo.persistor" :match {:action "find" :collection "albums"}}
                     
                     ;; Allow through any message with a field `wibble` with value `foo`.
                     {:match {:wibble "foo"}}]
          auth-outbound [
                     ;; Let through any messages coming from address 'ticker.mystock'
                     {:address "ticker.mystock"}
                     
                     ;;Let through any messages from addresses starting with "news." (e.g. news.europe, news.usa, etc)
                     {:address_re "news\\..+"}]
        (sockjs/bridge 
          (sockjs/sockjs-server http-server)
          {:prefix "/eventbus"} auth-inbound auth-outbound) 
        (http/listen http-server 8080 "localhost"))

To let all messages through you can specify two arrays with a single
empty JSON object which will match all messages.

     (sockjs/bridge sockjs-server {:prefix "/eventbus"} [{}] [{}]) 

**Be very careful!**

## Messages that require authorisation

The bridge can also refuse to let certain messages through if the user
is not authorised.

To enable this you need to make sure an instance of the
`vertx.auth-mgr` module is available on the event bus. (Please see the
modules manual for a full description of modules).

To tell the bridge that certain messages require authorisation before
being passed, you add the field `:requires_auth` with the value of
`true` in the match. The default value is `false`. For example, the
following match:

    {
      :address "demo.persistor"
      :match {
        :action "find"
        :collection "albums"
      }
      :requires_auth true
    }
    
This tells the bridge that any messages to find orders in the `albums`
collection, will only be passed if the user is successful
authenticated (i.e. logged in ok) first.

# File System

Vert.x lets you manipulate files on the file system. File system
operations are asynchronous and take a handler function as the last
argument.

This function will be called when the operation is complete, or an error
has occurred.

The first argument passed into the function is an exception-map, if an error
occurred. This will be `nil` if the operation completed
successfully. If the operation returns a result that will be passed in
the second argument to the handler.

The asynchronous file system functions are provided by the
`vertx.filesystem` namespace.

## Synchronous forms

For convenience, we also provide synchronous forms of most
operations. It's highly recommended the asynchronous forms are always
used for real applications.

The synchronous form does not take a handler as an argument and
returns its results directly. The name of the synchronous function is
the same as the name as the asynchronous form, but resides in the
`vertx.filesystem.sync` namespace.

## copy

Copies a file.

This function can be called in two different ways:

* `(copy source destination handler)`

Non-recursive file copy. `source` is the source file
name. `destination` is the destination file name.

Here's an example:
    (copy "foo.dat" "bar.dat"
      (fn [err]
        (if-not err
          (pritnln "Copy was successful"))))

* `(copy source destination recursive? handler)`

Recursive copy. `source` is the source file name. `destination` is the
destination file name. `recursive?` is a boolean flag - if `true` and
source is a directory, then a recursive copy of the directory and all
its contents will be attempted.

## move

Moves a file.

`(move source destination handler)`

`source` is the source file name. `destination` is the destination
file name. `handler` is called with the error, or `nil` if successful.

## truncate

Truncates a file.

`(truncate file len handler)`

`file` is the file name of the file to truncate. `len` is the length
in bytes to truncate it to. `handler` is called with the error, or
`nil` if successful.

## chmod

Changes permissions on a file or directory.

This function can be called in two different ways:

* `(chmod file perms handler)`.

Change permissions on a file.

`file` is the file name. `perms` is a Unix style permissions string
made up of 9 characters. The first three are the owner's
permissions. The second three are the group's permissions and the
third three are others permissions. In each group of three if the
first character is `r` then it represents a read permission. If the
second character is `w` it represents write permission. If the third
character is `x` it represents execute permission. If the entity does
not have the permission the letter is replaced with `-`. Some
examples:

    rwxr-xr-x
    r--r--r--

* `(chmod file perms dir-perms handler)`.

Recursively change permissions on a directory. `file` is the directory
name. `perms` is a Unix style permissions to apply recursively to any
files in the directory. `dir-perms` is a Unix style permissions string
to apply to the directory and any other child directories recursively.

`handler` is called with the error, or `nil` if successful.

## properties

Retrieve properties of a file or symbolic link.

This function can be called in two different ways:

* `(properties file handler)`

`file` is the file name. `handler` is called with the error (`nil` if
successful), along with a map of properties:

* `:creation-time`: Time of file creation.
* `:last-access-time`: Time of last file access.
* `:last-modified-time`: Time file was last modified.
* `:directory?`: This will have the value `true` if the file is a
  directory.
* `:regular-file?`: This will have the value `true` if the file is a
  regular file (not symlink or directory).
* `:symbolic-link?`: This will have the value `true` if the file is a
  symbolic link.
* `:other?`: This will have the value `true` if the file is another
  type.

Links are followed by default.

* `(properties link follow-link? handler)`

`link` is the name of a symbolic link. If `follow-link?` is true,
properties for the file the link points to will be passed to
`handler`. Otherwise, the properties for the link will be passed.

Here's an example:

    (properties "some-file.txt"
      (fn [err props]
        (if err
            (println "Failed to retrieve file props:" err)
            (println "Last accessed:" (:last-access-time props)))))
        
## link

Create a link.

This function can be called in two different ways:

* `(link path existing handler)`

`path` is the name of the link. `existing` is the existing file
(i.e. where to point the link at). `handler` is called with the error,
or `nil` if successful.

This creates a symbolic link by default.

* `(link path existing symbolic? handler)`

If `symbolic?` is false, create a hard link.


## resolve-symlink

Reads a symbolic link. I.e returns the path representing the file that
the symbolic link specified by `link` points to.

`(resolve-symlink link handler)`

`link` is the name of the link to read. `handler` is called with the
error (`nil` if successful), along with the string path to the
resolved file.  An usage example would be:

## delete

Deletes a file or recursively deletes a directory.

This function can be called in two ways:

* `(delete file handler)`

Deletes a file. `file` is the file name. `handler` is called with the
error, or `nil` if successful. The deletion is not recursive by
default.

* `(delete file recursive? handler)`

If `recursive?` is `true`, it deletes a directory with name `file`,
recursively. Otherwise it just deletes a file.

## mkdir

Creates a directory.

This function can be called in three ways:

* `(mkdir dirname handler)`

Makes a new empty directory with name `dirname`, and default
permissions. `handler` is called with the error, or `nil` if
successful. This will not create any missing parent dirs by default.

* `(mkdir dirname create-parents? handler)`

If `create-parents?` is `true`, this creates a new directory and
creates any of its parents too. 

* `(mkdir dirname create-parents? perms handler)`

Like `(mkdir dirname create-parents? handler)`, but also allows
permissions for the newly created director(ies) to be
specified. `perms` is a Unix style permissions string as explained
earlier.

## read_dir

Reads a directory. I.e. lists the contents of the directory.

This function can be called in two ways:

* `(read-dir dir-name handler)`

Lists the contents of a directory. `handler` is called with the error
(`nil` if successful), along with a vector of file paths.

* `(read-dir dir-name filter handler)`

List only the contents of a directory which match the filter regex. Here's
an example which only lists files with an extension `txt` in a
directory:

    (read-dir "mydirectory" #".*\.txt"
      (fn [err files]
        (when !err
          (println "Directory contains these .txt files:")
          (mapv println files))))
    
## read-file

Read the entire contents of a file in one go. *Be careful if using
this with large files since the entire file will be stored in memory
at once*.

`(read-file file handler)`

Where `file` is the file name of the file to read. `handler` is called
with the error (`nil` if successful), along with a `Buffer` containing
the file contents.

## write-file

Writes data to a new file on disk. 

`(write-file file data handler)` 

Where `file` is the file name. `data` is anything bufferable.
`handler` is called with the error, or `nil` if successful.

## create_file

Creates a new empty file.

This method can be called in two ways:

* `(create-file file handler)`

Where `file` is the file name. `handler` is called with the error, or
`nil` if successful. 

The file is created with the default permissions.

* `(create-file file perms handler)`

`perms` is a permission string as discussed earlier.

## exists?

Checks if a file exists.

`(exists? file handler)`

Where `file` is the file name. `handler` is called with the error
(`nil` if successful), along with a boolean.

## file-system-properties

Get properties for the file system.

`(file-system-properties file handler)`. 

Where `file` is any file on the file system. `handler` is called with the error
(`nil` if successful), along with a map of properties:

* `:total-space`. Total space on the file system in bytes.
* `:unallocated-space`. Unallocated space on the file system in bytes.
* `:usable-space`. Usable space on the file system in bytes.

## open

Opens an asynchronous file for reading \ writing.

(open file handler & kwargs)`

Where `file` is the file to be opened. `handler` is called with the error
(`nil` if successful), along with an AsyncFile object.

The behavior of the open call is further controlled by a set of
keyword arguments [default]:

* :create? - create the file if it does not already exist [true]
* :read?   - open the file for reading [true]
* :write?  - open the file for writing [true]
* :flush?  - the opened file will auto-flush writes [false]
* :perms   - the permissions used to create the file, if necessary
             (see create-file) [nil]

## AsyncFile

Instances of `AsyncFile` are returned from calls to `open` and you use
them to read from and write to files asynchronously. They allow
asynchronous random file access.

AsyncFile implements `ReadStream` and `WriteStream` so you can pump
files to and from other stream objects such as net sockets, http
requests and responses, and WebSockets.

They also allow you to read and write directly to them.

### Random access writes

To use an AsyncFile for random access writing you use the
`vertx.filesystem/write` method.

`(write file-obj data position handler)`.

The parameters to the function are: 

* `file-obj`: the `AsyncFile`.
* `data`: the data to write. Can be anything bufferable.
* `position`: an integer position in the file where to write the
  buffer. If the position is greater or equal to the size of the file,
  the file will be enlarged to accomodate the offset.
* `handler`: a function to call when the operation is
  complete. `handler` is called with the error, or `nil` if
  successful.

Here is an example of random access writes:

    (fs/open "some-file.dat"
      (fn [err file]
        (if err
          (println "Failed to open file" err)
          (let [data "ham-biscuit"]
            (dotimes [n 5]
              (fs/write file data (* (.length data) n)
                (fn [err]
                  (if err
                    (println "Failed to write" err)
                    (println "Written ok")))))))))
                    
### Random access reads

To use an AsyncFile for random access reads you use the `read`
function.

* `(read file-obj position length handler)`
* `(read file-obj buffer offset position length handler)`

The parameters to the method are: 

* `file-obj`: the `AsyncFile` to be read.
* `buffer`: the `Buffer` into which the data will be read. If not
  provided, a new `Buffer` will be created.
* `offset`: an integer offset into the given buffer where the read
  data will be placed.
* `position`: the position in the file where to read data from.
* `length`: the number of bytes of data to read
* `handler`: a function to call when the operation is complete.
  `handler` is called with the error, or `nil` if successful.

Here's an example of random access reads:
    
      (fs/open "some-file.dat"
      (fn [err file]
        (if err
          (println "Failed to open file" err)
          (let [buffer! (buf/buffer 1000)]
            (dotimes [n 10]
              (fs/write read buffer! (* n 100) (* n 100) 100
                (fn [err]
                  (if err
                    (println "Failed to read" err)
                    (println "Read ok")))))))))
                    
If you attempt to read past the end of file, the read will not fail
but it will simply read zero bytes.

### Flushing data to underlying storage.

If the AsyncFile was not opened with `:flush? true`, then you can
manually flush any writes from the OS cache by calling the `flush`
function.

### Using AsyncFile as `ReadStream` and `WriteStream`

AsyncFile implements `ReadStream` and `WriteStream` so you can then
use them with a pump to pump data to and from other read and write
streams.

Here's an example of pumping data from a file on a client to a HTTP
request:

    (let [client (http/client {:host "foo.com"})]
      (fs/open "some-file.dat"
        (fn [err file]
          (if err 
            (println "Failed to open file" err)
            (let [request 
                  (http/request client :PUT "/uploads"
                    (fn [resp]
                      (println "Response status code:" (.statusCode resp))))]
              ;; end the HTTP request when the file is fully sent
              (stream/on-end file (partial http/end request))
              (stream/pump file request))))))
            
### Closing an AsyncFile

To close an AsyncFile call the `close` function. Closing is asynchronous
and if you want to be notified when the close has been completed you
can specify a handler function as an argument to `close`.

# DNS Client

Often you will find yourself in situations where you need to obtain
DNS informations in an asynchronous fashion. Unfortunally this is not
possible with the API that is shipped with Java itself. Because of
this Vert.x offers it's own API for DNS resolution which is fully
asynchronous.

The DNS client functionality is provided by the `vertx.dns` namespace. 

All of the functions in the namespace return the DnsClient object that
was used to perform the lookup, and take several different
specifications of the servers to be used:

* a DnsClient object returned from another call
* a single InetSocketAddress object
* a String in the form "host" or "host:port" (port defaults to 53)
* a collection of InetSocketAddress objects
* a collection of "host"/"host:port" Strings

When collections are provided, they are tried in order, moving to the
next when the current server returns an error.

## lookup

Tries to lookup the A (ipv4) or AAAA (ipv6) record for a given
name. The first which is returned will be used, so it behaves the same
way as `nslookup`.
	
To lookup the A / AAAA record for "vertx.io" you would typically use it like:

    (require '[vertx.dns :as dns])
    
    (dns/lookup "10.0.0.1" "vertx.io"
                (fn [err r]
                   (if err
                     (println "ERROR:" (:type err))
                     (println (:address r)))))

You can also pass a type argument of `:ipv4` or `:ipv6` to constrain
the lookup to a particular IP version:

        (dns/lookup "10.0.0.1" "vertx.io" :ipv4
                (fn [err r] ...))
                
See the API documentation for more details.

## resolve

Tries to resolve various record types for a given name. This is quite
similar to using "dig" on unix like operating systems.

The type of record to be resolved must be one of: A, AAAA, CNAME, MX,
NS, PTR, SRV, or TXT.

The data passed to the handler function depends on the type of record
requested.
    
To lookup all the A records for "vertx.io" you would typically do:

    (dns/resolve ["10.0.0.1" "10.0.0.2"] :A "vertx.io"
                 (fn [err r]
                   (if err
                     (println "ERROR:" (:type err))
                     (doseq [x r] (println x)))))
    
See the API documentation for more details.

## reverseLookup

Tries to do a reverse lookup for an ip address. This is basically the
same as `resolve` for a PTR record, but allows you to just pass in the
ip address and not a valid PTR query string.

To do a reverse lookup for the ip address 127.0.0.1: 

    (dns/reverse-lookup ["10.0.0.1" "10.0.0.2"] "127.0.0.1"
                 (fn [err r]
                   (if err
                     (println "ERROR:" (:type err))
                     (println r))))
                     
See the API documentation for more details.

## Error handling

As you saw in previous sections, any error results in an exception-map
being passed as the first argument of the handler function. This
exception-map provides a `type` entry that specifies the error type.

The error types are:

* :NOERROR - No record was found for a given query
* :FORMERROR - Format error 
* :SERVFAIL - Server failure
* :NXDOMAIN - Name error
* :NOTIMPL - Not implemented by DNS Server
* :REFUSED - DNS Server refused the query
* :YXDOMAIN - Domain name should not exist
* :YXRRSET - Resource record should not exist
* :NXRRSET - RRSET does not exist
* :NOTZONE - Name not in zone
* :BADVER - Bad extension mechanism for version
* :BADSIG - Bad signature
* :BADKEY - Bad key
* :BADTIME - Bad timestamp

# Using nREPL

You can start nREPL endpoints within a verticle by calling the
`vertx.repl/start` function. By default, it binds to a random
port on localhost, or you can pass a port or port and host:

    (repl/start)

`start` returns the id of the nREPL server, and can be passed to 
`vertx.repl/stop` to shut it down. 

Any number of nREPL servers can be active at one time. The nREPL
servers run inside of a worker verticle, so code evaluated in the repl
won't block the event loop.






