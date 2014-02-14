/*
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.lang.clojure;

import org.projectodd.shimdandy.ClojureRuntimeShim;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;

public class ClojureVerticleFactory implements VerticleFactory {

    public ClojureVerticleFactory() {
        super();
    }

    @Override
    public void init(Vertx vertx, Container container, ClassLoader cl) {
        try {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            for(File each : (new File(cl.getResource("___runtime___").toURI())).listFiles()) {
                addURL.invoke(cl, each.toURI().toURL());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 URISyntaxException | MalformedURLException ignored) { ignored.printStackTrace();}

        this.runtime = ClojureRuntimeShim.newRuntime(new URLClassLoader(new URL[0], cl));
        this.ownsRuntime = (this.runtime.invoke("clojure.core/find-ns", 
                                                this.runtime.invoke("clojure.core/symbol", 
                                                                    "vertx.core")) == null);
        this.runtime.invoke("vertx.core/-bind-container-roots", vertx, container);
    }

    @Override
    public Verticle createVerticle(String scriptName) throws Exception {
        return new ClojureVerticle(scriptName, this.runtime);
    }

    @Override
    public void reportException(Logger logger, Throwable t) {
        log.error("Unexpected exception in Clojure verticle", t);
    }

    @Override
    public void close() {
        if (this.ownsRuntime) {
            this.runtime.invoke("clojure.core/shutdown-agents");
        }
    }

    private class ClojureVerticle extends Verticle {

        ClojureVerticle(String scriptName, ClojureRuntimeShim runtime) {
            this.scriptName = scriptName;
            this.runtime = runtime;
            this.id = UUID.randomUUID();
        }

        public void start() {
            this.runtime.invoke("vertx.core/-start-verticle", scriptName, this.id);
            log.info("Started clojure verticle: " + scriptName);
        }

        public void stop() {
            log.info("Stopping clojure verticle: " + scriptName);
            this.runtime.invoke("vertx.core/-stop-verticle", this.id);
        }
        private final String scriptName;
        private final UUID id;
        private final ClojureRuntimeShim runtime;
    }

    private static final Logger log = LoggerFactory.getLogger(ClojureVerticleFactory.class);
    private boolean ownsRuntime;
    private ClojureRuntimeShim runtime;

}
