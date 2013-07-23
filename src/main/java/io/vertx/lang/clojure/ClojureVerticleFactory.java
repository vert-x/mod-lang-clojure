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

import clojure.core.Vec;
import clojure.lang.Atom;
import clojure.lang.IFn;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import clojure.lang.RT;
import clojure.lang.Var;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleFactory;

import java.io.IOException;
import java.util.List;

public class ClojureVerticleFactory implements VerticleFactory {

    public ClojureVerticleFactory() {
        super();
    }

    @Override
    public void init(Vertx vertx, Container container, ClassLoader cl) {
        this.cl = cl;
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            RT.load("clojure/core");
            clojure.lang.Compiler.LOADER.bindRoot(this.cl);
            RT.var("vertx.core", "*vertx*").bindRoot(vertx);
            RT.var("vertx.core", "*container*").bindRoot(container);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    @Override
    public Verticle createVerticle(String scriptName) throws Exception {
        return new ClojureVerticle(scriptName);
    }

    @Override
    public void reportException(Logger logger, Throwable t) {
        log.error("Unexpected exception in Clojure verticle", t);
    }

    @Override
    public void close() {
        RT.var("clojure.core", "shutdown-agents").invoke();
    }

    private class ClojureVerticle extends Verticle {

        ClojureVerticle(String scriptName) {
            this.scriptName = scriptName;
        }

        public void start() {
            Var stopVar = RT.var("vertx.core", "!vertx-stop-fn").setDynamic();
            Var.pushThreadBindings(PersistentHashMap.create(stopVar, this.stopFn));
            try {
                RT.loadResourceScript(scriptName);
            } catch (Exception e) {
                throw new VertxException(e);
            } finally {
                Var.popThreadBindings();
            }
            log.info("Started clojure verticle: " + scriptName);
        }

        public void stop() {
            log.info("Stop verticle: " + scriptName);
            for(Object f : (List)this.stopFn.deref()) {
                ((IFn)f).invoke();
            }
        }
        private final String scriptName;
        private final Atom stopFn = new Atom(PersistentVector.create());
    }

    private static final Logger log = LoggerFactory.getLogger(ClojureVerticleFactory.class);
    private ClassLoader cl;

}
